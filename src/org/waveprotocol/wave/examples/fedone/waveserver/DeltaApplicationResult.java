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

import com.google.protobuf.ByteString;

import org.waveprotocol.wave.protocol.common.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

/**
 * Composes a ProtocolWaveletDelta and the result of its application, a
 * ProtocolAppliedWaveletDelta.
 *
 *
 */
public class DeltaApplicationResult {

  private final ProtocolAppliedWaveletDelta appliedDelta;
  private final ProtocolWaveletDelta delta;
  private final ProtocolHashedVersion hashedVersionAfterApplication;
  private final String error;
  private final ByteString appliedDeltaAsBytes;

  public DeltaApplicationResult(
      ProtocolAppliedWaveletDelta appliedDelta,
      ByteString appliedDeltaAsBytes,
      ProtocolWaveletDelta delta,
      ProtocolHashedVersion hashedVersionAfterApplication,
      String error) {
    this.appliedDelta = appliedDelta;
    this.appliedDeltaAsBytes = appliedDeltaAsBytes;
    this.delta = delta;
    this.hashedVersionAfterApplication = hashedVersionAfterApplication;
    this.error = error;
  }

  public ProtocolAppliedWaveletDelta getAppliedDelta() {
    return appliedDelta;
  }

  public ProtocolWaveletDelta getDelta() {
    return delta;
  }

  @Override
  public int hashCode() {
    return 31 * appliedDelta.hashCode() + delta.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DeltaApplicationResult)) {
      return false;
    } else {
      DeltaApplicationResult that = (DeltaApplicationResult) obj;
      return appliedDelta.equals(that.appliedDelta) && delta.equals(that.delta);
    }
  }

  public ProtocolHashedVersion getHashedVersionAfterApplication() {
    return hashedVersionAfterApplication;
  }

  public String getError() {
    return error;
  }

  public ByteString getAppliedDeltaAsBytes() {
    return appliedDeltaAsBytes;
  }
}
