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
package org.waveprotocol.box.common;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Constants;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

/**
 * An immutable sequence of transformed deltas.
 *
 * This class enforces that the deltas are contiguous.
 */
public final class DeltaSequence extends AbstractList<TransformedWaveletDelta> {
  private final ImmutableList<TransformedWaveletDelta> deltas;

  /**
   * Creates an empty delta sequence. This sequence will not have an end version.
   */
  public static DeltaSequence empty() {
    return new DeltaSequence(ImmutableList.<TransformedWaveletDelta>of());
  }

  /**
   * @param deltas to apply to a wavelet
   */
  public DeltaSequence(Iterable<TransformedWaveletDelta> deltas) {
    this.deltas = ImmutableList.copyOf(deltas);
    checkDeltaVersions();
  }

  /**
   * @throws IllegalArgumentException if any of the deltas' end version disagrees
   *         with the next delta's version.
   */
  private void checkDeltaVersions() {
    for (int i = 0; i < deltas.size(); i++) {
      TransformedWaveletDelta delta = deltas.get(i);
      long deltaEndVersion = delta.getResultingVersion().getVersion();
      if (i + 1 < deltas.size()) {
        long nextVersion = deltas.get(i + 1).getAppliedAtVersion();
        Preconditions.checkArgument(deltaEndVersion == nextVersion,
            "Delta %s / %s ends at version %s, expected %s",
            i + 1, deltas.size(), deltaEndVersion, nextVersion);
      }
    }
  }

  @Override
  public DeltaSequence subList(int start, int end) {
    List<TransformedWaveletDelta> subDeltas = deltas.subList(start, end);
    return new DeltaSequence(subDeltas);
  }

  /**
   * Constructs a DeltaSequence which consists of the specified deltas
   * followed by this sequence's deltas.
   */
  public DeltaSequence prepend(Iterable<TransformedWaveletDelta> prefixDeltas) {
    return new DeltaSequence(Iterables.concat(prefixDeltas, deltas));
  }

  public long getStartVersion() {
    return deltas.isEmpty() ? Constants.NO_VERSION : deltas.get(0).getAppliedAtVersion();
  }

  public HashedVersion getEndVersion() {
    Preconditions.checkState(!deltas.isEmpty(), "Empty delta sequence has no end version");
    return deltas.get(deltas.size() - 1).getResultingVersion();
  }

  @Override
  public boolean isEmpty() {
    return deltas.isEmpty();
  }

  @Override
  public int size() {
    return deltas.size();
  }

  @Override
  public TransformedWaveletDelta get(int index) {
    return deltas.get(index);
  }

  @Override
  public Iterator<TransformedWaveletDelta> iterator() {
    return deltas.iterator();
  }

  @Override
  public String toString() {
    return "[DeltaSequence " + deltas.size() + " deltas, v " + getStartVersion() + " -> "
        + getEndVersion() + ": " + deltas + "]";
  }
}
