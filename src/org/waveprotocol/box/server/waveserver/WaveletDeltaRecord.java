/**
 * Copyright 2010 Google Inc.
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

import com.google.common.base.Preconditions;
import com.google.inject.internal.Nullable;

import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;

/**
 * Bundles an applied delta (an original signed delta with information about how
 * it was applied) and its transformed operations.
 *
 * @author soren@google.com (Soren Lassen)
 */
public class WaveletDeltaRecord {
  @Nullable public final ByteStringMessage<ProtocolAppliedWaveletDelta> applied;
  public final TransformedWaveletDelta transformed;

  /**
   * @param applied is the applied delta which transforms to {@code transformed}
   * @param transformed is the transformed result of {@code applied}
   */
  public WaveletDeltaRecord(
      @Nullable ByteStringMessage<ProtocolAppliedWaveletDelta> applied,
      TransformedWaveletDelta transformed) {
    Preconditions.checkNotNull(transformed, "null transformed delta");
    this.applied = applied;
    this.transformed = transformed;
  }
}
