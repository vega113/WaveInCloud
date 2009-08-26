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

import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collections;
import java.util.List;

/**
 * A wavelet delta is a collection of {@link WaveletOperation}s from a single author.
 *
 *
 */
public class WaveletDelta {
  /** Author of the operations. */
  private final ParticipantId author;

  /** List of operations in the order they are to be applied. */
  private final List<? extends WaveletOperation> ops;

  /**
   * Create new delta from an author and a list of operations.
   *
   * @param author of the operations
   * @param ops list of operations
   */
  public WaveletDelta(ParticipantId author, List<? extends WaveletOperation> ops) {
    this.author = author;
    this.ops = ops;
  }

  public ParticipantId getAuthor() {
    return author;
  }

  /**
   * @return immutable list of the list of operations
   */
  public List<WaveletOperation> getOperations() {
    return Collections.unmodifiableList(ops);
  }

  @Override
  public int hashCode() {
    // (see Effective Java)
    int result = 17;

    result = 31 * result + author.hashCode();
    for (WaveletOperation op : ops) {
      result = 31 * result + op.hashCode();
    }

    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof WaveletDelta) {
      WaveletDelta wd = (WaveletDelta) obj;
      return author.equals(wd.author) && ops.equals(wd.ops);
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("WaveletDelta(" + author + ",");
    if (ops.isEmpty()) {
      stringBuilder.append("[]");
    } else {
      stringBuilder.append(" " + ops.size() + " ops: [" + ops.get(0));
      for (int i = 1; i < ops.size(); i++) {
        stringBuilder.append("," + ops.get(i));
      }
      stringBuilder.append("]");
    }
    stringBuilder.append(")");
    return stringBuilder.toString();
  }
}
