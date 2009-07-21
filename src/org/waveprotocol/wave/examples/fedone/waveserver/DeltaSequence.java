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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

import java.util.Iterator;
import java.util.List;

/**
 * A sequence of deltas along with the end version (after application of the
 * deltas).
 *
 *
 */
public class DeltaSequence implements Iterable<ProtocolWaveletDelta> {
  private final ImmutableList<ProtocolWaveletDelta> deltas;
  private final ProtocolHashedVersion endVersion;

  /**
   * @param deltas to apply to a wavelet
   * @param endVersion the version of the wavelet after all deltas were applied
   */
  DeltaSequence(Iterable<ProtocolWaveletDelta> deltas, ProtocolHashedVersion endVersion) {
    this.deltas = ImmutableList.copyOf(deltas);
    this.endVersion = endVersion;
    Preconditions.checkArgument(endVersion.getVersion() >= 0,
        "Expected endVersion >= 0, got " + endVersion.getVersion());
    checkDeltaVersions();
  }

  /**
   * @throws IllegalArgumentException if any of the deltas' end version disagrees
   *         with the next delta's version or (if it's the last delta), endVersion.
   */
  private void checkDeltaVersions() {
    for (int i = 0; i < deltas.size(); i++) {
      ProtocolWaveletDelta delta = deltas.get(i);
      long deltaEndVersion = delta.getHashedVersion().getVersion() + delta.getOperationCount();
      long nextVersion =
        ((i + 1 < deltas.size()) ? deltas.get(i + 1).getHashedVersion() : endVersion).getVersion();
      if (deltaEndVersion != nextVersion) {
        throw new IllegalArgumentException(
            String.format("Delta %d / %d ends at version %d, expected %d",
                i + 1, deltas.size(), deltaEndVersion, nextVersion));
      }
    }
  }

  static DeltaSequence empty(ProtocolHashedVersion version) {
    return new DeltaSequence(ImmutableList.<ProtocolWaveletDelta>of(), version);
  }

  /**
   * Constructs a DeltaSequence containing only a subSequence of this sequence's
   * deltas, indexed by position in the sequence.
   *
   * @param start The index of the first delta in {@link #getDeltas()} to
   *              include in the subsequence.
   * @param end One past the index of the last delta in {@link #getDeltas()} to
   *            include in the subsequence
   * @return A DeltaSequence that contains only {@code getGeltas().subList(start, end)}.
   */
  DeltaSequence subSequence(int start, int end) {
    List<ProtocolWaveletDelta> subDeltas = deltas.subList(start, end);
    ProtocolHashedVersion subEndVersion =
      (end == deltas.size()) ? deltas.get(end).getHashedVersion() : endVersion;
      return new DeltaSequence(subDeltas, subEndVersion);
  }

  /**
   * Constructs a DeltaSequence which consists of the specified deltas
   * followed by this sequence's deltas.
   */
  DeltaSequence prepend(Iterable<ProtocolWaveletDelta> prefixDeltas) {
    return new DeltaSequence(Iterables.concat(prefixDeltas, deltas), endVersion);
  }

  List<ProtocolWaveletDelta> getDeltas() {
    return deltas;
  }

  ProtocolHashedVersion getEndVersion() {
    return endVersion;
  }

  boolean isEmpty() {
    return deltas.isEmpty();
  }

  ProtocolHashedVersion getStartVersion() {
    return deltas.isEmpty() ? endVersion : deltas.get(0).getHashedVersion();
  }

  @Override
  public Iterator<ProtocolWaveletDelta> iterator() {
    return deltas.iterator();
  }

  @Override
  public String toString() {
    return String.format("%d delta%s (version %d .. %d): %s", deltas.size(),
        (deltas.size() == 1 ? "" : "s"),
        getStartVersion().getVersion(), getEndVersion().getVersion(), deltas);
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + deltas.hashCode();
    result = 31 * result + endVersion.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof DeltaSequence)) {
      return false;
    } else {
      DeltaSequence that = (DeltaSequence) obj;
      return endVersion.equals(that.endVersion) && deltas.equals(that.deltas);
    }
  }
}
