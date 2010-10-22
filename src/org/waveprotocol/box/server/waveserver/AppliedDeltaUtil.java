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

import com.google.protobuf.InvalidProtocolBufferException;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.common.HashedVersionFactoryImpl;
import org.waveprotocol.box.server.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;

/**
 * Utility methods for {@code ProtocolAppliedWaveletDelta}s.
 */
public class AppliedDeltaUtil {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());

  private static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionFactoryImpl(URI_CODEC);

  /**
   * Inspects the given applied delta to determine the {@code HashedVersion} it
   * was applied at.
   * This may require looking at the contained {@code ProtocolWaveletDelta}.
   *
   * @param appliedDelta to inspect
   * @return hashed version the delta was applied at
   * @throws InvalidProtocolBufferException if the contained
   *         {@code ProtocolWaveletDelta} is invalid
   *         (is only inspected if the applied delta has the hashed version set)
   */
  public static HashedVersion getHashedVersionAppliedAt(
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDeltaBytes)
      throws InvalidProtocolBufferException {
    ProtocolAppliedWaveletDelta appliedDelta = appliedDeltaBytes.getMessage();
    return CoreWaveletOperationSerializer.deserialize(
        // If the delta was transformed, the version it was actually applied at is specified
        // in the top-level message, otherwise we take if from the original signed delta.
        appliedDelta.hasHashedVersionAppliedAt()
        ? appliedDelta.getHashedVersionAppliedAt()
        : ProtocolWaveletDelta.parseFrom(appliedDelta.getSignedOriginalDelta().getDelta())
              .getHashedVersion());
  }

  /**
   * Calculates the hashed version after an applied delta is applied.
   */
  public static HashedVersion calculateResultingHashedVersion(
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta)
      throws InvalidProtocolBufferException {
    return HASH_FACTORY.create(
        appliedDelta.getByteArray(),
        getHashedVersionAppliedAt(appliedDelta),
        appliedDelta.getMessage().getOperationsApplied());
  }

  private AppliedDeltaUtil() { } // prevent instantiation
}
