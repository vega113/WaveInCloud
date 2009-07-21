// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.examples.fedone.common;

import org.waveprotocol.wave.model.id.WaveletName;

/**
 * Factory for hashed versions.
 *
 *
 */
public interface HashedVersionFactory {
  HashedVersion createVersionZero(WaveletName waveletName);
  HashedVersion create(byte[] appliedDeltaBytes, HashedVersion hashedVersionAppliedAt,
      int operationsApplied);
  HashedVersion create(long version, byte[] historyHash);
}
