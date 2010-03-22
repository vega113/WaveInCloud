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
 * @author zdwang@google.com (David Wang)
 * @author anorth@google.com (Alex North)
 */
public interface IdSerialiser {
  /**
   * Checked exception indicating that a serialised wave or wavelet id is
   * invalid.
   */
  public static class InvalidIdException extends Exception {
    private final String id;
    public InvalidIdException(String id, String message) {
      super(message);
      this.id = id;
    }

    public String getId() {
      return id;
    }

    @Override
    public String getMessage() {
      return "Invalid id '" + id + "': " + super.getMessage();
    }
  }

  /**
   * Runtime exception indicating that a serialised wave or wavelet id is
   * invalid. It's an @link {@link IllegalArgumentException} but more specific
   * so that it may be caught without catching other IAEs.
   */
  public static class RuntimeInvalidIdException extends IllegalArgumentException {
    private final String id;
    public RuntimeInvalidIdException(String id, String message) {
      super(message);
      this.id = id;
    }

    public String getId() {
      return id;
    }

    @Override
    public String getMessage() {
      return "Invalid id '" + id + "': " + super.getMessage();
    }
  }

  /** Separates a wave id from a wavelet id in serialised form. */
  public static final char PART_SEPARATOR = '!';

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
