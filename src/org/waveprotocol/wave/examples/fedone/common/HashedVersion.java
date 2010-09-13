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

package org.waveprotocol.wave.examples.fedone.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.apache.commons.codec.binary.Hex;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;

import java.util.Arrays;

/**
 * A {@link CoreWaveletDelta}'s version along with a hash of the preceding history.
 * For fake deltas that do not contain the correct hash, use
 * {@link #unsigned(long)}.
 *
 * This is an immutable value class and therefore thread safe.
 *
 *
 */
public final class HashedVersion implements Comparable<HashedVersion> {
  /** Same as unsigned(0). */
  public static final HashedVersion UNSIGNED_VERSION_0 = unsigned(0);

  /**
   * Constructs an unhashed (i.e. unsigned) version with only a version number
   * and some fake hash. This may be used when we don't rely on hashes
   * for authentication and error checking.
   */
  @VisibleForTesting
  public static HashedVersion unsigned(long version) {
    return new HashedVersion(version, new byte[0]);
  }

  /**
   * Lexicographic comparison of two byte arrays.
   *
   * @return -1, 0, or 1
   */
  private static int compare(byte[] first, byte[] second) {
    if (first == second) {
      return 0; // no need to compare contents
    }
    for (int i = 0; i < first.length && i < second.length; i++) {
      if (first[i] != second[i]) {
        return Integer.signum(first[i] - second[i]);
      }
    }
    // Bytes are equal up to the length of the shortest array. Then longest is bigger.
    return Integer.signum(first.length - second.length);
  }

  private final long version;
  private final byte[] historyHash;

  /**
   * Constructs a hashed version with the specified version and historyHash.
   */
  public HashedVersion(long version, byte[] historyHash) {
    Preconditions.checkNotNull(historyHash, "null historyHash");
    this.version = version;
    this.historyHash = historyHash;
  }

  /**
   * {@inheritDoc}
   *
   * Lexicographic comparison of version and historyHash.
   */
  @Override
  public int compareTo(HashedVersion other) {
    return version != other.version
        ? Long.signum(version - other.version)
        : compare(historyHash, other.historyHash);
  }

  /** The number of ops that lead to this version. */
  public long getVersion() {
    return version;
  }

  /** A hash over the entire history of ops up to this version. */
  public byte[] getHistoryHash() {
    return historyHash;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + Long.valueOf(version).hashCode();
    result = 31 * result + Arrays.hashCode(historyHash);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof HashedVersion)) {
      return false;
    } else {
      HashedVersion that = (HashedVersion) obj;
      return version == that.version && Arrays.equals(historyHash, that.historyHash);
    }
  }

  @Override
  public String toString() {
    return Long.toString(version) + ":" + new String(Hex.encodeHex(historyHash));
  }
}
