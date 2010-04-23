/**
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.wave.model.operation;

import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.Transformer;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * The class for transforming operations as in the Jupiter system.
 *
 * The Jupiter algorithm takes 2 operations S and C and produces S' and C'.
 * Where the operations, S + C' = C + S'
 */
public class Transform {

  /**
   * Transforms a pair of operations.
   *
   * TODO: remove this as soon as all callers have been changed to call
   * transform(WaveletOperation, ParticipantId, WaveletOperation, ParticipantId)
   * and pass in the proper clientOpAuthor and serverOpAuthor
   *
   * @param clientOp The client's operation.
   * @param serverOp The server's operation.
   * @return The resulting transformed client and server operations.
   * @throws TransformException if a problem was encountered during the
   *         transformation.
   */
  @Deprecated
  public static OperationPair<WaveletOperation> transform(WaveletOperation clientOp,
      WaveletOperation serverOp) throws TransformException {
    ParticipantId dummyId = new ParticipantId("");
    return transform(clientOp, dummyId, serverOp, dummyId);
  }

  /**
   * Transforms a pair of operations.
   *
   * @param clientOp The client's operation.
   * @param clientOpAuthor The author of the client's operation.
   * @param serverOp The server's operation.
   * @param serverOpAuthor The author of the server's operation.
   * @return The resulting transformed client and server operations.
   * @throws TransformException if a problem was encountered during the
   *         transformation.
   */
  public static OperationPair<WaveletOperation> transform(WaveletOperation clientOp,
      ParticipantId clientOpAuthor, WaveletOperation serverOp, ParticipantId serverOpAuthor)
      throws TransformException {
    if (clientOp instanceof WaveletDocumentOperation && serverOp instanceof
        WaveletDocumentOperation) {
      WaveletDocumentOperation clientWaveDocOp = (WaveletDocumentOperation) clientOp;
      WaveletDocumentOperation serverWaveDocOp = (WaveletDocumentOperation) serverOp;
      if (clientWaveDocOp.getDocumentId().equals(serverWaveDocOp.getDocumentId())) {
        // Transform document operations
        BufferedDocOp clientMutation = clientWaveDocOp.getOperation();
        BufferedDocOp serverMutation = serverWaveDocOp.getOperation();
        OperationPair<BufferedDocOp> transformedDocOps =
          Transformer.transform(clientMutation, serverMutation);
        clientOp = new WaveletDocumentOperation(clientWaveDocOp.getDocumentId(),
            transformedDocOps.clientOp());
        serverOp = new WaveletDocumentOperation(serverWaveDocOp.getDocumentId(),
            transformedDocOps.serverOp());
      } else {
        // Different documents don't conflict; use identity transform below
      }
    } else {

      if (serverOp instanceof RemoveParticipant) {
        RemoveParticipant serverRemoveOp = (RemoveParticipant) serverOp;
        if (serverRemoveOp.getParticipantId().equals(clientOpAuthor)) {
          // clientOpAuthor has issued a client operation that is concurrent with a server
          // operation to remove clientOpAuthor, hence the client operation is doomed
          throw new RemovedAuthorException(clientOpAuthor.getAddress());
        }
        if (clientOp instanceof RemoveParticipant) {
          RemoveParticipant clientRemoveOp = (RemoveParticipant) clientOp;
          if (clientRemoveOp.getParticipantId().equals(serverRemoveOp.getParticipantId())) {
            clientOp = NoOp.INSTANCE;
            serverOp = NoOp.INSTANCE;
          }
        } else if (clientOp instanceof AddParticipant) {
          checkParticipantRemovalAndAddition(serverRemoveOp, (AddParticipant) clientOp);
        }
      } else if (serverOp instanceof AddParticipant) {
        AddParticipant serverAddOp = (AddParticipant) serverOp;
        if (clientOp instanceof AddParticipant) {
          AddParticipant clientAddOp = (AddParticipant) clientOp;
          if (clientAddOp.getParticipantId().equals(serverAddOp.getParticipantId())) {
            clientOp = NoOp.INSTANCE;
            serverOp = NoOp.INSTANCE;
          }
        } else if (clientOp instanceof RemoveParticipant) {
          checkParticipantRemovalAndAddition((RemoveParticipant) clientOp, serverAddOp);
        }
      }
    }
    // Apply identity transform by default
    return new OperationPair<WaveletOperation>(clientOp, serverOp);
  }

  /**
   * Checks to see if a participant is being removed by one operation and added
   * by another concurrent operation. In such a situation, at least one of the
   * operations is invalid.
   *
   * @param removeParticipant The operation to remove a participant.
   * @param addParticipant The operation to add a participant.
   * @throws TransformException if the same participant is being concurrently
   *         added and removed.
   */
  private static void checkParticipantRemovalAndAddition(RemoveParticipant removeParticipant,
      AddParticipant addParticipant) throws TransformException {
    ParticipantId participantId = removeParticipant.getParticipantId();
    if (participantId.equals(addParticipant.getParticipantId())) {
      throw new TransformException("Transform error involving participant: " +
          participantId.getAddress());
    }
  }

}
