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
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.commons.codec.binary.Hex;
import org.waveprotocol.wave.examples.fedone.model.util.HashedVersionFactoryImpl;
import org.waveprotocol.wave.examples.fedone.waveserver.ByteStringMessage;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.protocol.common.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

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

  /**
   * Get the hashed version an applied delta was applied at.
   */
  public static HashedVersion getHashedVersionAppliedAt(
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta) {
    if (appliedDelta.getMessage().hasHashedVersionAppliedAt()) {
      return WaveletOperationSerializer.deserialize(appliedDelta.getMessage()
          .getHashedVersionAppliedAt());
    } else {
      try {
        ProtocolWaveletDelta innerDelta = ProtocolWaveletDelta.parseFrom(appliedDelta.getMessage().getSignedOriginalDelta().getDelta());
        return WaveletOperationSerializer.deserialize(innerDelta.getHashedVersion());
      } catch (InvalidProtocolBufferException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  /**
   * Get the hashed version after an applied delta is applied.
   */
  public static HashedVersion getHashedVersionAfter(
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta) {
    int opsApplied = appliedDelta.getMessage().getOperationsApplied();
    HashedVersion versionAppliedAt = getHashedVersionAppliedAt(appliedDelta);
    return new HashedVersionFactoryImpl().create(
        appliedDelta.getByteArray(), versionAppliedAt, opsApplied);
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
