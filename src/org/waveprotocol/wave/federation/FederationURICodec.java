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

package org.waveprotocol.wave.federation;

import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.URIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.PercentEncoderDecoder;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * URI encoder and decoder for Federation. Helper class wrapping
 * IdURIEncoderDecoder.
 *
 * TODO(thorogood): Rethink the future of this class - move to static methods,
 * or singleton model.
 *
 * @author thorogood@google.com (Sam Thorogood)
 */
public class FederationURICodec {
  private final IdURIEncoderDecoder internalCodec;

  public FederationURICodec() {
    internalCodec = new IdURIEncoderDecoder(new PercentEncoderDecoder() {

      @Override
      public String decode(String encodedValue) throws EncodingException {
        try {
          encodedValue = encodedValue.replace("+", "%2B");
          String result = URLDecoder.decode(encodedValue, "UTF-8");
          if (result.indexOf(0xFFFD) != -1) {
            throw new URIEncoderDecoder.EncodingException(
                "Unable to decode value, it contains invalid UTF-8: " + encodedValue);
          }
          return result;
        } catch (UnsupportedEncodingException e) {
          throw new URIEncoderDecoder.EncodingException(e);
        }
      }

      @Override
      public String encode(String decodedValue) throws EncodingException {
        try {
          return URLEncoder.encode(decodedValue, "UTF-8");
        } catch (UnsupportedEncodingException e) {
          throw new URIEncoderDecoder.EncodingException(e);
        }
      }

    });
  }

  /**
   * Encode a WaveletName class to its string representation, including
   * "wave://".
   *
   * TODO(thorogood): Stop throwing {@link IllegalArgumentException} to remove
   * the risk of crashing servers
   */
  public String encode(WaveletName waveletName) {
    try {
      return internalCodec.waveletNameToURI(waveletName);
    } catch (EncodingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Decode a WaveletName class from its string representation, requires
   * "wave://".
   *
   * TODO(thorogood): Stop throwing {@link IllegalArgumentException} to remove
   * the risk of crashing servers
   */
  public WaveletName decode(String waveletName) {
    try {
      return internalCodec.uriToWaveletName(waveletName);
    } catch (EncodingException e) {
      throw new IllegalArgumentException(e);
    }
  }

}
