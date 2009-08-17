/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.wave.examples.fedone.waveserver;

import org.waveprotocol.wave.protocol.common.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

/**
 * Composes a ProtocolWaveletDelta and the result of its application, the canonical representation
 * of a {@code ProtocolAppliedWaveletDelta} as a {@code ByteStringMessage}.
 *
 *
 */
public class DeltaApplicationResult {

  private final ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta;
  private final ProtocolWaveletDelta transformedDelta;
  private final ProtocolHashedVersion hashedVersionAfterApplication;

  /**
   * @param appliedDelta result of application, untransformed
   * @param transformedDelta result of application, transformed
   * @param hashedVersionAfterApplication
   */
  public DeltaApplicationResult(
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta,
      ProtocolWaveletDelta transformedDelta,
      ProtocolHashedVersion hashedVersionAfterApplication) {
    this.appliedDelta = appliedDelta;
    this.transformedDelta = transformedDelta;
    this.hashedVersionAfterApplication = hashedVersionAfterApplication;
  }

  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDelta() {
    return appliedDelta;
  }

  public ProtocolWaveletDelta getDelta() {
    return transformedDelta;
  }

  @Override
  public int hashCode() {
    return 31 * appliedDelta.hashCode() + transformedDelta.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DeltaApplicationResult)) {
      return false;
    } else {
      DeltaApplicationResult that = (DeltaApplicationResult) obj;
      return appliedDelta.equals(that.appliedDelta)
          && transformedDelta.equals(that.transformedDelta);
    }
  }

  public ProtocolHashedVersion getHashedVersionAfterApplication() {
    return hashedVersionAfterApplication;
  }
}
