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

package org.waveprotocol.box.server.waveserver;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;

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
  public WaveletDeltaRecord submitRequest(WaveletName waveletName,
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
   * @return the transformed and applied delta.
   * @throws OperationException if an error occurs during transformation or
   *         application
   * @throws InvalidProtocolBufferException if the signed delta did not contain a valid delta
   * @throws InvalidHashException if delta hash sanity checks fail
   */
  private WaveletDeltaRecord transformAndApplyLocalDelta(ProtocolSignedDelta signedDelta)
      throws OperationException, InvalidProtocolBufferException, InvalidHashException {
    ProtocolWaveletDelta protocolDelta =
        ByteStringMessage.parseProtocolWaveletDelta(signedDelta.getDelta()).getMessage();

    Preconditions.checkArgument(protocolDelta.getOperationCount() > 0, "empty delta");

    WaveletDelta transformed = maybeTransformSubmittedDelta(
        CoreWaveletOperationSerializer.deserialize(protocolDelta));

    // TODO(ljvderijk): a Clock needs to be injected here (Issue 104)
    long applicationTimestamp = System.currentTimeMillis();

    // This is always false right now because the current algorithm doesn't transform ops away.
    if (transformed.size() == 0) {
      Preconditions.checkState(currentVersion.getVersion() != 0,
          "currentVersion can not be 0 if delta was transformed");
      Preconditions.checkState(
          transformed.getTargetVersion().getVersion() <= currentVersion.getVersion());
      // The delta was transformed away. That's OK but we don't call either
      // applyWaveletOperations(), because that will throw IllegalArgumentException, or
      // commitAppliedDelta(), because empty deltas cannot be part of the delta history.
      TransformedWaveletDelta emptyDelta = new TransformedWaveletDelta(transformed.getAuthor(),
          transformed.getTargetVersion(), applicationTimestamp, transformed);
      return new WaveletDeltaRecord(null, emptyDelta);
    }

    if (!transformed.getTargetVersion().equals(currentVersion)) {
      Preconditions.checkState(
          transformed.getTargetVersion().getVersion() < currentVersion.getVersion());
      // The delta was a duplicate of an existing server delta.
      // We duplicate-eliminate it (don't apply it to the wavelet state and don't store it in
      // the delta history) and return the server delta which it was a duplicate of
      // (so delta submission becomes idempotent).
      ByteStringMessage<ProtocolAppliedWaveletDelta> existingDeltaBytes =
          lookupAppliedDelta(transformed.getTargetVersion());
      TransformedWaveletDelta dupDelta = lookupTransformedDelta(transformed.getTargetVersion());
      // TODO(anorth): Replace these comparisons with methods on delta classes.
      Preconditions.checkState(dupDelta.getAuthor().equals(transformed.getAuthor()),
          "Duplicate delta detected but mismatched author, expected %s found %s",
          transformed.getAuthor(), dupDelta.getAuthor());
      Preconditions.checkState(dupDelta.getOperations().equals(transformed.getOperations()),
          "Duplicate delta detected but mismatched ops, expected %s found %s",
          transformed, dupDelta.getOperations());

      return new WaveletDeltaRecord(existingDeltaBytes, dupDelta);
    }

    // Build the applied delta to commit
    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta =
        buildAppliedDelta(signedDelta, currentVersion, transformed.size(), applicationTimestamp);

    return buildAndApplyDelta(transformed.getAuthor(), appliedDelta, transformed);
  }

  @VisibleForTesting
  static ByteStringMessage<ProtocolAppliedWaveletDelta> buildAppliedDelta(
      ProtocolSignedDelta signedDelta, HashedVersion appliedAtVersion, int operationsApplied,
      long applicationTimestamp) {
    ProtocolAppliedWaveletDelta.Builder appliedDeltaBuilder = ProtocolAppliedWaveletDelta
        .newBuilder()
        .setSignedOriginalDelta(signedDelta)
        .setOperationsApplied(operationsApplied)
        .setApplicationTimestamp(applicationTimestamp);
    // TODO: re-enable this condition for version 0.3 of the spec
    if (/* opsWereTransformed */true) {
      // This is set to indicate the head version of the wavelet was different
      // to the intended version of the wavelet (so the hash will have changed)
      appliedDeltaBuilder.setHashedVersionAppliedAt(
          CoreWaveletOperationSerializer.serialize(appliedAtVersion));
    }
    return ByteStringMessage.serializeMessage(appliedDeltaBuilder.build());
  }

  @Override
  public boolean isDeltaSigner(HashedVersion version, ByteString signerId) {
    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta =
        lookupAppliedDeltaByEndVersion(version);
    if (appliedDelta == null) {
      return false;
    }
    ProtocolSignedDelta signedDelta = appliedDelta.getMessage().getSignedOriginalDelta();
    for (ProtocolSignature signature : signedDelta.getSignatureList()) {
      if (signature.getSignerId().equals(signerId)) return true;
    }
    return false;
  }
}
