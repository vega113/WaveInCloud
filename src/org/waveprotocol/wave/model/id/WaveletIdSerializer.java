/**
 * Copyright 2010 Google Inc.
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

import org.waveprotocol.wave.model.util.Serializer;

/**
 * Serializer for wavelet ids which uses WaveletId#seralise()
 *
 * @author anorth@google.com (Alex North)
 */
public final class WaveletIdSerializer {

  /**
   * Singleton instance
   */
  public static final Serializer<WaveletId> INSTANCE = new Serializer<WaveletId>() {
    @Override
    public WaveletId fromString(String s) {
      return fromString(s, null);
    }

    /**
     * Deserialize a wavelet id string, gracefully handling legacy
     * serializations without a domain.
     *
     * @param s serialized wavelet id
     * @return wavelet id represented by {@code s}
     */
    private WaveletId deserializeLegacy(String s) {
      try {
        return WaveletId.deserialise(s);
      } catch (IllegalArgumentException e) {
        // The string may be a legacy wavelet id, that is missing a domain part.
        if (!s.contains("" + LegacyIdSerialiser.PART_SEPARATOR)) {
          return WaveletId.deserialise("google.com" + LegacyIdSerialiser.PART_SEPARATOR + s);
        } else {
          throw e;
        }
      }
    }

    @Override
    public WaveletId fromString(String s, WaveletId defaultValue) {
      return (s != null) ? deserializeLegacy(s) : defaultValue;
    }

    @Override
    public String toString(WaveletId x) {
      return (x != null) ? x.serialise() : null;
    }
  };

  private WaveletIdSerializer() {}
}
