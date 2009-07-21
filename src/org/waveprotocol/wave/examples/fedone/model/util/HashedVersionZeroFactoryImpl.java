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

package org.waveprotocol.wave.examples.fedone.model.util;

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactory;
import org.waveprotocol.wave.examples.fedone.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;

import java.nio.charset.Charset;

/**
 * Utility class for creating Hashed Versions, the "lightweight" base class can only
 * calculate the version 0 hash and is suitable for use in the client.
 *
 *
 *
 */
public class HashedVersionZeroFactoryImpl implements HashedVersionFactory {

  private final IdURIEncoderDecoder URI_CODEC = new IdURIEncoderDecoder(
      new URLEncoderDecoderBasedPercentEncoderDecoder());

  private static final Charset CHAR_SET = Charset.forName("UTF-8");

  /**
   * Create a new HashedVersion for version 0 of the given wavelet.
   * @param waveletName the name of wavelet.
   * @throws IllegalArgumentException if a bad wavelet name is passed.
   * @return new HashedVersion at version 0.
   */
  @Override
  public HashedVersion createVersionZero(WaveletName waveletName) {
    try {
      // Same encoding as used protobuf/CodedOutputSteam to serialize a String to byte[].
      // http://code.google.com/p/protobuf/source/browse/trunk/java/src/main/java/com/google/protobuf/CodedOutputStream.java
      byte[] historyHash = URI_CODEC.waveletNameToURI(waveletName).getBytes(CHAR_SET);
      return new HashedVersion(0, historyHash);
    } catch (EncodingException e) {
      throw new IllegalArgumentException("Bad wavelet name " + waveletName, e);
    }
  }

  /** Explicitly fail for creating a hashed version with version not 0. */
  @Override
  public HashedVersion create(byte[] appliedDeltaBytes, HashedVersion hashedVersionAppliedAt,
      int operationsApplied) {
    // For lightweight users of this Factory, don't depend on crypto code.
    throw new UnsupportedOperationException("This method is not supported here.");
  }

  @Override
  public HashedVersion create(long version, byte[] historyHash) {
    return new HashedVersion(version, historyHash);
  }
}
