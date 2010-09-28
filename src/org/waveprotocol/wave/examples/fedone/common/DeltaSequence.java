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
package org.waveprotocol.wave.examples.fedone.common;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.waveprotocol.wave.examples.common.HashedVersion;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

/**
 * A sequence of deltas along with the end version (after application of the
 * deltas).
 */
public final class DeltaSequence extends AbstractList<VersionedWaveletDelta> {
  private final ImmutableList<VersionedWaveletDelta> deltas;
  private final HashedVersion endVersion;

  public static DeltaSequence empty(HashedVersion version) {
    return new DeltaSequence(ImmutableList.<VersionedWaveletDelta>of(), version);
  }

  /**
   * @param deltas to apply to a wavelet
   * @param endVersion the version of the wavelet after all deltas were applied
   */
  public DeltaSequence(Iterable<VersionedWaveletDelta> deltas, HashedVersion endVersion) {
    this.deltas = ImmutableList.copyOf(deltas);
    this.endVersion = endVersion;
    Preconditions.checkArgument(endVersion.getVersion() >= 0,
        "Expected endVersion >= 0, got %s", endVersion.getVersion());
    checkDeltaVersions();
  }

  /**
   * @throws IllegalArgumentException if any of the deltas' end version disagrees
   *         with the next delta's version or (if it's the last delta), endVersion.
   */
  private void checkDeltaVersions() {
    for (int i = 0; i < deltas.size(); i++) {
      VersionedWaveletDelta delta = deltas.get(i);
      long deltaEndVersion = delta.version.getVersion() + delta.delta.getOperations().size();
      long nextVersion =
          ((i + 1 < deltas.size()) ? deltas.get(i + 1).version : endVersion)
          .getVersion();
      Preconditions.checkArgument(deltaEndVersion == nextVersion,
          "Delta %s / %s ends at version %s, expected %s",
          i + 1, deltas.size(), deltaEndVersion, nextVersion);
    }
  }

  @Override
  public DeltaSequence subList(int start, int end) {
    List<VersionedWaveletDelta> subDeltas = deltas.subList(start, end);
    HashedVersion subEndVersion = (end == deltas.size()) ? endVersion : deltas.get(end).version;
    return new DeltaSequence(subDeltas, subEndVersion);
  }

  /**
   * Constructs a DeltaSequence which consists of the specified deltas
   * followed by this sequence's deltas.
   */
  public DeltaSequence prepend(Iterable<VersionedWaveletDelta> prefixDeltas) {
    return new DeltaSequence(Iterables.concat(prefixDeltas, deltas), endVersion);
  }

  public List<VersionedWaveletDelta> getDeltas() {
    return deltas;
  }

  public HashedVersion getStartVersion() {
    return deltas.isEmpty() ? endVersion : deltas.get(0).version;
  }

  public HashedVersion getEndVersion() {
    return endVersion;
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
  public VersionedWaveletDelta get(int index) {
    return deltas.get(index);
  }

  @Override
  public Iterator<VersionedWaveletDelta> iterator() {
    return deltas.iterator();
  }

  @Override
  public String toString() {
    return String.format("%d delta%s (version %s .. %s): %s", deltas.size(),
        (deltas.size() == 1 ? "" : "s"),
        getStartVersion(), getEndVersion(), deltas);
  }
}
