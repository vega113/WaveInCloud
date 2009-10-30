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

import org.waveprotocol.wave.protocol.common.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

/**
 * Utility methods for {@code ProtocolAppliedWaveletDelta}s.
 */
public class AppliedDeltaUtil {

  private AppliedDeltaUtil() {
  }
  
  /**
   * Inspect the given applied delta to determine the {@code ProtocolHashedVersion} it was
   * applied at.  This may require looking at the contained {@code ProtocolWaveletDelta}.
   * 
   * @param appliedDelta to inspect
   * @return hashed version the delta was applied at
   * @throws IllegalArgumentException if the contained {@code ProtocolWaveletDelta} is invalid
   *         (may not be inspected if the applied delta has the hashed version set)
   */
  public static ProtocolHashedVersion getHashedVersionAppliedAt(
      ProtocolAppliedWaveletDelta appliedDelta) {
    if (appliedDelta.hasHashedVersionAppliedAt()) {
      return appliedDelta.getHashedVersionAppliedAt();
    } else {
      // Delta wasn't transformed, hashed version applied at comes from the contained delta
      try {
        ProtocolWaveletDelta delta =
          ProtocolWaveletDelta.parseFrom(appliedDelta.getSignedOriginalDelta().getDelta());
        return delta.getHashedVersion();
      } catch (InvalidProtocolBufferException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }
}
