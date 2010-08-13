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

package org.waveprotocol.wave.examples.webclient.common;

import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;

/**
 * A {@link CoreWaveletDelta}'s version along with a hash of the preceding history.
 * For fake deltas that do not contain the correct hash, use
 * {@link #unsigned(long)}.
 *
 * This is an immutable value class and therefore thread safe.
 *
 * This is the GWT-specific version of this class.
 *
 *
 */
public final class HashedVersion {
  /** Same as unsigned(0). */
  public static final HashedVersion UNSIGNED_VERSION_0 = unsigned(0);

//  private static final HashedVersionFactory HASHED_HISTORY_VERSION_FACTORY =
//    new HashedVersionZeroFactoryImpl();

  private final long version;
  private final String historyHash;

  /**
   * Constructs a hashed version with the specified version and historyHash.
   */
  public HashedVersion(long version, String historyHash) {
    if (historyHash == null) {
      throw new NullPointerException("null historyHash");
    } else {
      this.version = version;
      this.historyHash = historyHash;
    }
  }

//  /**
//   * Constructs a HashedVersion representing version 0 of the specified wavelet.
//   */
//  public static HashedVersion versionZero(WaveletName waveletName) {
//    // TODO: Why not inline that method here and get rid of the class?
//    return HASHED_HISTORY_VERSION_FACTORY.createVersionZero(waveletName);
//  }

  /**
   * Constructs an unhashed (i.e. unsigned) version with only a version number
   * and some fake hash. This may be used when we don't rely on hashes
   * for authentication and error checking.
   */
  public static HashedVersion unsigned(long version) {
    return new HashedVersion(version, "\0");
  }



  /** The number of ops that lead to this version. */
  public long getVersion() {
    return version;
  }

  /** A hash over the entire history of ops up to this version. */
  public String getHistoryHash() {
    return historyHash;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + Long.valueOf(version).hashCode();
    result = 31 * result + historyHash.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof HashedVersion)) {
      return false;
    } else {
      HashedVersion that = (HashedVersion) obj;
      return version == that.version && historyHash.equals(that.historyHash);
    }
  }

  @Override
  public String toString() {
    return Long.toString(version) + ":" + encodeHex(historyHash);
  }

  private String encodeHex(final String historyHash) {
    StringBuilder out = new StringBuilder();
    for (char c: historyHash.toCharArray()) {
      out.append(Integer.toHexString(c));
    }
    return out.toString();
  }
}
