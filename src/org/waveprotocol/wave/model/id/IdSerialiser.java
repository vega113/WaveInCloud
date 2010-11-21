/**
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.waveprotocol.wave.model.id;

import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;

/**
 * Serialises identifiers and waverefs to and from strings.
 *
 * @author zdwang@google.com (David Wang)
 * @author anorth@google.com (Alex North)
 */
public interface IdSerialiser {
  /** Separates a wave id from a wavelet id in serialised form. */
  public static final char PART_SEPARATOR = '!';
  /** Separates parts of a waveref in serialised form. */
  public static final String WAVEREF_PART_SEPARATOR = "/";

  /**
   * Serialises a wave id into a string.
   */
  String serialiseWaveId(WaveId waveId);

  /**
   * Serialises a wavelet id into a string.
   */
  String serialiseWaveletId(WaveletId waveletId);

  /**
   * Deserialises a wave id encoded in a string.
   *
   * @throws InvalidIdException if the serialised id is invalid
   */
  WaveId deserialiseWaveId(String serialisedForm) throws InvalidIdException;

  /**
   * @throws InvalidIdException if the serialised id is invalid
   */
  WaveletId deserialiseWaveletId(String serialisedForm) throws InvalidIdException;
}
