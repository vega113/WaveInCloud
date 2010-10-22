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

package org.waveprotocol.box.agents.echoey;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.waveprotocol.box.client.ClientUtils;
import org.waveprotocol.box.common.DocumentConstants;
import org.waveprotocol.box.server.agents.agent.AbstractAgent;
import org.waveprotocol.box.server.agents.agent.AgentConnection;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Map;
import java.util.Set;

/**
 * Example agent that echoes back operations.
 */
public class Echoey extends AbstractAgent {
  static final String FAREWELL = "Goodbye.";
  static final String GREETING = "I'm listening.";
  static final char[] PASSWORD = "".toCharArray();

  private static final Log LOG = Log.get(Echoey.class);

  private final Map<WaveletName, Set<String>> documentsSeen = new MapMaker().makeComputingMap(
      new Function<WaveletName, Set<String>>() {
        @Override
        public Set<String> apply(WaveletName waveletName) {
          return Sets.newHashSet();
        }
      });

  /**
   * @return the suffix that Echoey adds to each document it is editing
   */
  @VisibleForTesting
  String getEchoeyDocumentSuffix() {
    return IdConstants.TOKEN_SEPARATOR + getParticipantId().getAddress();
  }

  /**
   * Returns a notification message for when a participant is added to a wavelet that Echoey
   * participates in.
   *
   * @param participant id of the participant that was added
   * @return the message to be sent
   */
  @VisibleForTesting
  String getParticipantAddedMessage(ParticipantId participant) {
    return participant.getAddress() + " was added to this wavelet.";
  }

  /**
   * Returns a notification message for when a participant is removed from a wavelet that Echoey
   * participates in.
   *
   * @param participant id of the participant that was removed
   * @return the message to be sent
   */
  @VisibleForTesting
  String getParticipantRemovedMessage(ParticipantId participant) {
    return participant.getAddress() + " was removed from this wavelet.";
  }

  /**
   * Main entry point.
   *
   * @param args program arguments.
   */
  public static void main(String[] args) {
    try {
      if (args.length == 3) {
        int port;
        try {
          port = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Must provide valid port.");
        }

        Echoey agent = new Echoey(AgentConnection.newConnection(args[0], PASSWORD, args[1], port));
        agent.run();
      } else {
        System.out.println("usage: java Echoey <username> <hostname> <port>");
      }
    } catch (Exception e) {
      LOG.severe("Catastrophic failure", e);
      System.exit(1);
    }

    System.exit(0);
  }

  /**
   * Constructor.
   *
   * @param connection the agent's connection to the server.
   */
  @Inject
  @VisibleForTesting
  Echoey(AgentConnection connection) {
    super(connection);
  }

  @Override
  public void onDocumentChanged(WaveletData wavelet, String docId, BufferedDocOp docOp) {
    WaveletName waveletName = WaveletDataUtil.waveletNameOf(wavelet);
    LOG.info("onDocumentChanged: " + waveletName + ", " + docId);

    if (docId.equals(DocumentConstants.MANIFEST_DOCUMENT_ID)) {
      // Don't echo anything on the manifest document
    } else if (docId.endsWith(getEchoeyDocumentSuffix())) {
      // Don't echo any document that we created
    } else {
      String echoDocId = docId + getEchoeyDocumentSuffix();

      // Echo the change to the other document documentOperation.
      CoreWaveletOperation[] ops = new CoreWaveletOperation[] {
          new CoreWaveletDocumentOperation(echoDocId, docOp)
      };

      // Write the document into the manifest if it isn't already there
      if (documentsSeen.get(waveletName).add(docId)) {
        BlipData manifest = wavelet.getDocument(DocumentConstants.MANIFEST_DOCUMENT_ID);
        ops = new CoreWaveletOperation[] {
          ops[0],
          ClientUtils.appendToManifest(manifest, echoDocId)
        };

      }

      sendAndAwaitWaveletOperations(waveletName, ops);
    }
  }

  /**
   * Append a new blip to a wavelet with the given contents.
   */
  private void appendText(WaveletData wavelet, String text) {
    String docId = getNewDocumentId() + getEchoeyDocumentSuffix();
    CoreWaveletOperation[] ops = ClientUtils.createAppendBlipOps(
        wavelet.getDocument(DocumentConstants.MANIFEST_DOCUMENT_ID), docId,
        text);
    sendAndAwaitWaveletOperations(WaveletDataUtil.waveletNameOf(wavelet), ops);
  }

  @Override
  public void onParticipantAdded(WaveletData wavelet, ParticipantId participant) {
    LOG.info("onParticipantAdded: " + participant.getAddress());
    appendText(wavelet, getParticipantAddedMessage(participant));
  }

  @Override
  public void onParticipantRemoved(WaveletData wavelet, ParticipantId participant) {
    LOG.info("onParticipantRemoved: " + participant.getAddress());
    appendText(wavelet, getParticipantRemovedMessage(participant));
  }

  @Override
  public void onSelfAdded(WaveletData wavelet) {
    LOG.info("onSelfAdded: " + WaveletDataUtil.waveletNameOf(wavelet));
    appendText(wavelet, GREETING);
  }

  @Override
  public void onSelfRemoved(WaveletData wavelet) {
    LOG.info("onSelfRemoved: " + WaveletDataUtil.waveletNameOf(wavelet));
    appendText(wavelet, FAREWELL);
  }
}
