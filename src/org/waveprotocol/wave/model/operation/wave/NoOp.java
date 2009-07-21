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

package org.waveprotocol.wave.model.operation.wave;

import org.waveprotocol.wave.model.wave.data.WaveletData;

/**
 * Operation class for a no-op.
 *
 *
 */
public final class NoOp extends WaveletOperation {

  private static final int HASH = NoOp.class.getName().hashCode();

  /**
   * Constructs a no-op.
   */
  public NoOp() {}

  /**
   * Does nothing.
   */
  @Override
  protected void doApply(WaveletData target) {
    // do nothing.
  }

  @Override
  public String toString() {
    return "no-op " + super.toString();
  }

  @Override
  public int hashCode() {
    /*
     * NOTE(alexmah): We may be able to get rid of this hash function in the
     * future if this class becomes a singleton.
     */
    return HASH;
  }

  @Override
  public boolean equals(Object obj) {
    /*
     * NOTE(alexmah): We're ignoring context in equality comparison. The plan is
     * to remove context from all operations in the future.
     */
    return obj instanceof NoOp;
  }

}
