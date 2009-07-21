// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.examples.fedone.common;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.examples.fedone.model.util.HashedVersionFactoryImpl;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;

import java.util.Arrays;

/**
 * A {@link WaveletDelta}'s version along with a hash of the preceding history.
 * For fake deltas that do not contain the correct hash, use
 * {@link #unsigned(long)}.
 *
 * This is an immutable value class and therefore thread safe.
 *
 *
 */
public final class HashedVersion {
  /** Same as unsigned(0). */
  public static final HashedVersion UNSIGNED_VERSION_0 = unsigned(0);

  private static final HashedVersionFactory HASHED_HISTORY_VERSION_FACTORY =
    new HashedVersionFactoryImpl();

  private final long version;
  private final byte[] historyHash;

  /**
   * Constructs a hashed version with the specified version and historyHash.
   */
  public HashedVersion(long version, byte[] historyHash) {
    if (historyHash == null) {
      throw new NullPointerException("null historyHash");
    } else {
      this.version = version;
      this.historyHash = historyHash;
    }
  }

  /**
   * Constructs a HashedVersion representing version 0 of the specified wavelet.
   */
  public static HashedVersion versionZero(WaveletName waveletName) {
    // TODO: Why not inline that method here and get rid of the class?
    return HASHED_HISTORY_VERSION_FACTORY.createVersionZero(waveletName);
  }

  /**
   * Constructs an unhashed (i.e. unsigned) version with only a version number
   * and some fake hash. This may be used when we don't rely on hashes
   * for authentication and error checking.
   */
  @VisibleForTesting
  public static HashedVersion unsigned(long version) {
    return new HashedVersion(version, new byte[0]);
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
    result = 31 * result + historyHash.hashCode();
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
    return Long.toString(version) + "@" + historyHash;
  }
}
