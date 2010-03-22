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

import org.waveprotocol.wave.model.id.IdSerialiser.InvalidIdException;
import org.waveprotocol.wave.model.id.IdSerialiser.RuntimeInvalidIdException;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * In the context of a single wave, a wavelet is identified by a tuple of a
 * wave provider domain and a local identifier which is unique within the domain
 * and the wave.
 *
 * @author zdwang@google.com (David Wang)
 * @author anorth@google.com (Alex North)
 */
public final class WaveletId implements Comparable<WaveletId> {

  private final String domain;
  private final String id;

  /**
   * Deserialises a wavelet identifier from a string, throwing a checked exception
   * if deserialisation fails.
   *
   * @param waveletIdString a serialised wave id
   * @return a wave id
   * @throws InvalidIdException if the serialised form is invalid
   */
  public static WaveletId checkedDeserialise(String waveletIdString) throws InvalidIdException {
    return LongIdSerialiser.INSTANCE.deserialiseWaveletId(waveletIdString);
  }

  /**
   * Deserialises a wave identifier from a string.
   *
   * @param waveletIdString a serialised wave id
   * @return a wavelet id
   * @throws RuntimeInvalidIdException if the serialised form is invalid
   */
  public static WaveletId deserialise(String waveletIdString) throws RuntimeInvalidIdException {
    try {
      return checkedDeserialise(waveletIdString);
    } catch (InvalidIdException e) {
      throw new RuntimeInvalidIdException(e.getId(), e.getMessage());
    }
  }

  /**
   * Checks a string is a valid serialised wavelet id.
   *
   * @param waveletIdString serialised wavelet id
   * @return the input string, for convenience
   * @throws RuntimeInvalidIdException if the string is invalid
   */
  public static String checkIsValid(String waveletIdString) throws RuntimeInvalidIdException {
    deserialise(waveletIdString);
    return waveletIdString;
  }

  /**
   * @param domain must not be null. This is assumed to be of a valid canonical
   *        domain format.
   * @param id must not be null. This is assumed to be escaped with
   *        SimplePrefixEscaper.DEFAULT_ESCAPER.
   */
  public WaveletId(String domain, String id) {
    if (domain == null || id == null) {
      Preconditions.nullPointer("Cannot create WaveletId with null value in [domain:"
          + domain + "] [id:" + id + "]");
    }
    if (domain.isEmpty() || id.isEmpty()) {
      Preconditions.illegalArgument("Cannot create wave id with empty value in [domain:"
          + domain + "] [id:" + id + "]");
    }

    if (SimplePrefixEscaper.DEFAULT_ESCAPER.hasEscapeCharacters(domain)) {
      Preconditions.illegalArgument(
          "Domain cannot contain characters that requires escaping: " + domain);
    }

    if (!SimplePrefixEscaper.DEFAULT_ESCAPER.isEscapedProperly(IdConstants.TOKEN_SEPARATOR, id)) {
      Preconditions.illegalArgument("Id is not properly escaped: " + id);
    }

    // Intern domain string for memory efficiency.
    // NOTE(anorth): Update equals() if interning is removed.
    this.domain = domain.intern();
    this.id = id;
  }

  /**
   * @return the domain
   */
  public String getDomain() {
    return domain;
  }

  /**
   * @return the local id
   */
  public String getId() {
    return id;
  }

  /**
   * Serialises this waveletId into a unique string. For any two wavelet ids,
   * waveletId1.serialise().equals(waveletId2.serialise()) iff waveId1.equals(waveId2).
   */
  public String serialise() {
    return LongIdSerialiser.INSTANCE.serialiseWaveletId(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + domain.hashCode();
    result = prime * result + id.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    WaveletId other = (WaveletId) obj;
    // Reference equality as domains are interned at construction.
    return (domain == other.domain) && id.equals(other.id);
  }

  @Override
  public String toString() {
    return "[WaveletId:" + serialise() + "]";
  }

  @Override
  public int compareTo(WaveletId other) {
    int domainCompare = domain.compareTo(other.domain);
    if (domainCompare == 0) {
      return id.compareTo(other.id);
    } else {
      return domainCompare;
    }
  }
}
