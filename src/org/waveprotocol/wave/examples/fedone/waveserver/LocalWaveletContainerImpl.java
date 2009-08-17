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

package org.waveprotocol.wave.examples.fedone.waveserver;

import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.WaveletOperationSerializer;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.protocol.common.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.protocol.common.ProtocolSignedDelta;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

import java.util.List;

/**
 * A local wavelet may be updated by submits. The local wavelet will perform
 * operational transformation on the submitted delta and assign it the latest
 * version of the wavelet.
 */
class LocalWaveletContainerImpl extends WaveletContainerImpl
    implements LocalWaveletContainer {

  public LocalWaveletContainerImpl(WaveletName waveletName) {
    super(waveletName);
  }

  @Override
  public DeltaApplicationResult submitRequest(WaveletName waveletName,
      ProtocolSignedDelta signedDelta) throws OperationException,
      InvalidProtocolBufferException, InvalidHashException {

    acquireWriteLock();
    try {
      return transformAndApplyLocalDelta(signedDelta);
    } finally {
      releaseWriteLock();
    }
  }

  /**
   * Apply a signed delta to a local wavelet. This assumes the caller has
   * validated that the delta is at the correct version and can be applied to
   * the wavelet. Must be called with writelock held.
   *
   * @param signedDelta the delta that is to be applied to wavelet.
   * @return transformed operations are applied to this delta.
   * @throws OperationException if an error occurs during transformation or
   *         application
   * @throws InvalidProtocolBufferException if the signed delta did not contain a valid delta
   * @throws InvalidHashException if delta hash sanity checks fail
   *
   * @throws OperationException
   */
  private DeltaApplicationResult transformAndApplyLocalDelta(ProtocolSignedDelta signedDelta)
      throws OperationException, InvalidProtocolBufferException,
      InvalidHashException {

    ByteStringMessage<ProtocolWaveletDelta> protocolDelta = ByteStringMessage.from(
        ProtocolWaveletDelta.getDefaultInstance(), signedDelta.getDelta());
    Pair<WaveletDelta, HashedVersion> deltaAndVersion =
      WaveletOperationSerializer.deserialize(protocolDelta.getMessage());

    // Transform operations against the current version.  We need to track whether the operations
    // were actually transformed in order to set up the applied delta protobuf properly.
    List<WaveletOperation> transformedOps =
        maybeTransformSubmittedDelta(deltaAndVersion.first, deltaAndVersion.second);
    boolean opsWereTransformed = (transformedOps != null);
    if (transformedOps == null) {
      transformedOps = deltaAndVersion.first.getOperations();
    }

    // Operations are allowed to fail, just ignore any ops after the one which did
    int opsApplied = applyWaveletOperations(transformedOps);
    if (opsApplied != transformedOps.size()) {
      transformedOps = transformedOps.subList(0, opsApplied);
    }

    // Serialize applied delta with the old "current" version, giving the canonical version of
    // of the applied delta as a ByteString (so that the hash applies to the bytes)
    ProtocolAppliedWaveletDelta.Builder appliedDeltaBuilder =
        ProtocolAppliedWaveletDelta.newBuilder()
            .setSignedOriginalDelta(signedDelta)
            .setOperationsApplied(transformedOps.size())
            .setApplicationTimestamp(System.currentTimeMillis());

    // TODO: re-enable this condition for version 0.3 of the spec
    if (/*opsWereTransformed*/ true) {
      // This is set to indicate the head version of the wavelet was different to the intended
      // version of the wavelet (so the hash will have changed)
      appliedDeltaBuilder.setHashedVersionAppliedAt(
          WaveletOperationSerializer.serialize(currentVersion));
    }

    ProtocolAppliedWaveletDelta protocolAppliedDelta = appliedDeltaBuilder.build();
    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta = ByteStringMessage.from(
        ProtocolAppliedWaveletDelta.getDefaultInstance(), protocolAppliedDelta.toByteString());

    return commitAppliedDelta(appliedDelta,
        new WaveletDelta(deltaAndVersion.first.getAuthor(), transformedOps));
  }
}
