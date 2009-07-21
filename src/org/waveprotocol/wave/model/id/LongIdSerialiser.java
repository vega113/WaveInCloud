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

package org.waveprotocol.wave.model.id;

/**
 * Serialises and deserialises wave ids and wavelet ids to and from
 * the format &lt;domain&gt;!&lt;id&gt;.
 *
 *
 */
public class LongIdSerialiser implements IdSerialiser {

  public static final IdSerialiser INSTANCE = new LongIdSerialiser();

  @Override
  public String serialiseWaveId(WaveId waveId) {
    return waveId.getDomain() + PART_SEPARATOR + waveId.getId();
  }

  @Override
  public String serialiseWaveletId(WaveletId waveletId) {
    return waveletId.getDomain() + PART_SEPARATOR + waveletId.getId();
  }

  @Override
  public WaveId deserialiseWaveId(String serialisedForm) {
    String[] parts = SimplePrefixEscaper.DEFAULT_ESCAPER.splitWithoutUnescaping(
        PART_SEPARATOR, serialisedForm);
    if (parts.length != 2) {
      throw new IllegalArgumentException("Unable to deserialise the long wave id: " +
          serialisedForm + ". The wave id need to look like <domain>" + PART_SEPARATOR + "<id>");
    } else {
      return new WaveId(parts[0], parts[1]);
    }
  }

  @Override
  public WaveletId deserialiseWaveletId(String serialisedForm) {
    String[] parts = SimplePrefixEscaper.DEFAULT_ESCAPER.splitWithoutUnescaping(
        PART_SEPARATOR, serialisedForm);
    if (parts.length != 2) {
      throw new IllegalArgumentException("Unable to deserialise the long wavelet id: " +
          serialisedForm + ". The wavelet id need to look like <domain>" + PART_SEPARATOR + "<id>");
    } else {
      return new WaveletId(parts[0], parts[1]);
    }
  }
}
