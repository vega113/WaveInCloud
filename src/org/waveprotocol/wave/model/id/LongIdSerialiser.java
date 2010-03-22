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
 * @author zdwang@google.com (David Wang)
 */
public class LongIdSerialiser implements IdSerialiser {

  public static final LongIdSerialiser INSTANCE = new LongIdSerialiser();

  @Override
  public String serialiseWaveId(WaveId waveId) {
    return waveId.getDomain() + PART_SEPARATOR + waveId.getId();
  }

  @Override
  public String serialiseWaveletId(WaveletId waveletId) {
    return waveletId.getDomain() + PART_SEPARATOR + waveletId.getId();
  }

  @Override
  public WaveId deserialiseWaveId(String serialisedForm) throws InvalidIdException {
    String[] parts = SimplePrefixEscaper.DEFAULT_ESCAPER.splitWithoutUnescaping(
        PART_SEPARATOR, serialisedForm);
    if ((parts.length != 2) || parts[0].isEmpty() || parts[1].isEmpty()) {
      throw new InvalidIdException(serialisedForm,
          "Wave id must be of the form <domain>" + PART_SEPARATOR + "<id>");
    } else {
      return new WaveId(parts[0], parts[1]);
    }
  }

  @Override
  public WaveletId deserialiseWaveletId(String serialisedForm) throws InvalidIdException {
    String[] parts = SimplePrefixEscaper.DEFAULT_ESCAPER.splitWithoutUnescaping(
        PART_SEPARATOR, serialisedForm);
    if ((parts.length != 2) || parts[0].isEmpty() || parts[1].isEmpty()) {
      throw new InvalidIdException(serialisedForm,
          "Wavelet id must be of the form <domain>" + PART_SEPARATOR + "<id>");
    } else {
      return new WaveletId(parts[0], parts[1]);
    }
  }
}
