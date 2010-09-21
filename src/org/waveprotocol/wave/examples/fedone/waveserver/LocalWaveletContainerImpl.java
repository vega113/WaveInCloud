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

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.wave.examples.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.common.VersionedWaveletDelta;
import org.waveprotocol.wave.examples.fedone.util.EmptyDeltaException;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;

/**
 * A local wavelet may be updated by submits. The local wavelet will perform
 * operational transformation on the submitted delta and assign it the latest
 * version of the wavelet.
 */
class LocalWaveletContainerImpl extends WaveletContainerImpl
    implements LocalWaveletContainer {

  private static final Log LOG = Log.get(LocalWaveletContainerImpl.class);

  /**
   * Associates a delta (represented by the hashed version of the wavelet state after its
   * application) with its signers (represented by their signer id)
   */
  private final Multimap<ProtocolHashedVersion, ByteString> deltaSigners =
      ArrayListMultimap.create();

  public LocalWaveletContainerImpl(WaveletName waveletName) {
    super(waveletName);
  }

  @Override
  public DeltaApplicationResult submitRequest(WaveletName waveletName,
      ProtocolSignedDelta signedDelta) throws OperationException,
      InvalidProtocolBufferException, InvalidHashException, EmptyDeltaException {
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
      throws OperationException, InvalidProtocolBufferException, EmptyDeltaException,
      InvalidHashException {
    ByteStringMessage<ProtocolWaveletDelta> protocolDelta = ByteStringMessage.from(
        ProtocolWaveletDelta.getDefaultInstance(), signedDelta.getDelta());
    VersionedWaveletDelta deltaAndVersion =
      CoreWaveletOperationSerializer.deserialize(protocolDelta.getMessage());

    if (deltaAndVersion.delta.getOperations().isEmpty()) {
      LOG.warning("No operations to apply at version " + deltaAndVersion.version);
      throw new EmptyDeltaException();
    }

    VersionedWaveletDelta transformed = maybeTransformSubmittedDelta(deltaAndVersion);

    // TODO(ljvderijk): a Clock needs to be injected here (Issue 104)
    long applicationTimeStamp = System.currentTimeMillis();

    // This is always false right now because the current algorithm doesn't transform ops away.
    if (transformed.delta.getOperations().isEmpty()) {
      Preconditions.checkState(currentVersion.getVersion() != 0,
          "currentVersion can not be 0 if delta was transformed");
      Preconditions.checkState(transformed.version.getVersion() <= currentVersion.getVersion());
      // The delta was transformed away. That's OK but we don't call either
      // applyWaveletOperations(), because that will throw EmptyDeltaException, or
      // commitAppliedDelta(), because empty deltas cannot be part of the delta history.
      return new DeltaApplicationResult(buildAppliedDelta(signedDelta, transformed,
          applicationTimeStamp),
          CoreWaveletOperationSerializer.serialize(transformed.delta, transformed.version),
          CoreWaveletOperationSerializer.serialize(transformed.version));
    }

    if (!transformed.version.equals(currentVersion)) {
      Preconditions.checkState(transformed.version.getVersion() < currentVersion.getVersion());
      // The delta was a duplicate of an existing server delta.
      // We duplicate-eliminate it (don't apply it to the wavelet state and don't store it in
      // the delta history) and return the server delta which it was a duplicate of
      // (so delta submission becomes idem-potent).
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta =
          lookupAppliedDelta(transformed.version);
      // TODO: look this up (in appliedDeltas or currentVersion) rather than compute it?
      HashedVersion hashedVersionAfterApplication = HASH_FACTORY.create(
        appliedDelta.getByteArray(), transformed.version, transformed.delta.getOperations().size());
      return new DeltaApplicationResult(appliedDelta,
          CoreWaveletOperationSerializer.serialize(transformed.delta, transformed.version),
          CoreWaveletOperationSerializer.serialize(hashedVersionAfterApplication));
    }

    applyWaveletOperations(transformed.delta, applicationTimeStamp);

    // Build the applied delta to commit
    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta =
        buildAppliedDelta(signedDelta, transformed, applicationTimeStamp);

    DeltaApplicationResult applicationResult = commitAppliedDelta(appliedDelta, transformed.delta);

    // Associate this hashed version with its signers.
    for (ProtocolSignature signature : signedDelta.getSignatureList()) {
      deltaSigners.put(
          applicationResult.getHashedVersionAfterApplication(),
          signature.getSignerId());
    }

    return applicationResult;
  }

  private static ByteStringMessage<ProtocolAppliedWaveletDelta> buildAppliedDelta(
      ProtocolSignedDelta signedDelta, VersionedWaveletDelta transformed,
      long applicationTimeStamp) {
    ProtocolAppliedWaveletDelta.Builder appliedDeltaBuilder = ProtocolAppliedWaveletDelta
        .newBuilder()
        .setSignedOriginalDelta(signedDelta)
        .setOperationsApplied(transformed.delta.getOperations().size())
        .setApplicationTimestamp(applicationTimeStamp);
    // TODO: re-enable this condition for version 0.3 of the spec
    if (/* opsWereTransformed */true) {
      // This is set to indicate the head version of the wavelet was different
      // to the intended
      // version of the wavelet (so the hash will have changed)
      appliedDeltaBuilder.setHashedVersionAppliedAt(
          CoreWaveletOperationSerializer.serialize(transformed.version));
    }
    return ByteStringMessage.fromMessage(appliedDeltaBuilder.build());
  }

  @Override
  public boolean isDeltaSigner(ProtocolHashedVersion version, ByteString signerId) {
    return deltaSigners.get(version).contains(signerId);
  }
}
