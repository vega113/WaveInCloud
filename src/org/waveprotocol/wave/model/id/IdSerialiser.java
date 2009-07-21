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
 * This allows us to serialize and deserialise ids.
 *
 *
 */
public interface IdSerialiser {
  /** Separates a wave id from a wavelet id in serialised form. */
  public static final char PART_SEPARATOR = '!';

  /**
   * Turn a wave id into a string. If the domain of the id is the same as this,
   * then the serialised form does not contain the domain.
   */
  String serialiseWaveId(WaveId waveId);

  /**
   * Turn a wavelet id into a string. If the domain of the id is the same as this,
   * then the serialised form does not contain the domain.
   */
  String serialiseWaveletId(WaveletId waveletId);

  /**
   * Turn a string into a wavel id. If the domain is not specified in the string, then the
   * wave id returned has the default domain.
   */
  WaveId deserialiseWaveId(String serialisedForm);

  /**
   * Turn a string into a wavel id. If the domain is not specified in the string, then the
   * wave id returned has the default domain.
   */
  WaveletId deserialiseWaveletId(String serialisedForm);

}
