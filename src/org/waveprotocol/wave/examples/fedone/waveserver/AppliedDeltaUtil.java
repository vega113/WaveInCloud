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

import org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactory;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactoryImpl;
import org.waveprotocol.wave.examples.fedone.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;

/**
 * Utility methods for {@code ProtocolAppliedWaveletDelta}s.
 */
public class AppliedDeltaUtil {

  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());

  private static final HashedVersionFactory HASHED_HISTORY_VERSION_FACTORY =
      new HashedVersionFactoryImpl(URI_CODEC);

  /**
   * Inspects the given applied delta to determine the {@code ProtocolHashedVersion} it was
   * applied at.  This may require looking at the contained {@code ProtocolWaveletDelta}.
   *
   * @param appliedDelta to inspect
   * @return hashed version the delta was applied at
   * @throws InvalidProtocolBufferException if the contained {@code ProtocolWaveletDelta} is invalid
   *         (may not be inspected if the applied delta has the hashed version set)
   */
  public static ProtocolHashedVersion getHashedVersionAppliedAt(
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta)
      throws InvalidProtocolBufferException {
    if (appliedDelta.getMessage().hasHashedVersionAppliedAt()) {
      return appliedDelta.getMessage().getHashedVersionAppliedAt();
    } else {
      // Delta wasn't transformed, hashed version applied at comes from the contained delta
      return ProtocolWaveletDelta
          .parseFrom(appliedDelta.getMessage().getSignedOriginalDelta().getDelta())
          .getHashedVersion();
    }
  }

  /**
   * Calculates the hashed version after an applied delta is applied.
   */
  public static ProtocolHashedVersion calculateHashedVersionAfter(
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta)
      throws InvalidProtocolBufferException {
    return createHashedVersion(
        appliedDelta.getByteArray(),
        getHashedVersionAppliedAt(appliedDelta),
        appliedDelta.getMessage().getOperationsApplied());
  }

  /**
   * Creates a {@link ProtocolHashedVersion};
   */
  private static ProtocolHashedVersion createHashedVersion(byte[] appliedDeltaBytes,
      ProtocolHashedVersion hashedVersionAppliedAt, int operationsApplied) {
    return CoreWaveletOperationSerializer.serialize(HASHED_HISTORY_VERSION_FACTORY.create(
        appliedDeltaBytes,
        CoreWaveletOperationSerializer.deserialize(hashedVersionAppliedAt),
        operationsApplied));
  }

  private AppliedDeltaUtil() { } // prevent instantiation
}
