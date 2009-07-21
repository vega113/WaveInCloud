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

package org.waveprotocol.wave.model.wave;

/**
 * A ParticipantId uniquely identifies a {@link Participant}. It looks like
 * an e-mail address, e.g. 'joe@gmail.com'
 *
 *
 */
public final class ParticipantId {

  /** The participant's address */
  private final String address;

  /**
   * Constructs an id
   *
   * @param address a non-null address string
   */
  public ParticipantId(String address) {
    if (address == null) {
      throw new NullPointerException();
    }
    this.address = normalize(address);
  }

  /**
   * Normalises an address.
   *
   * @param address  address to normalise; may be null
   * @return normal form of {@code address} if non-null; null otherwise.
   */
  private static String normalize(String address) {
    if (address == null) {
      return null;
    }
    return address.toLowerCase();
  }

  /**
   * @return the participant's address
   */
  public String getAddress() {
    return address;
  }

  /**
   * @return the domain name in the address. If no "@" occurs, it will be the whole string,
   *     if more than one occurs, it will be the part after the last "@".
   */
  public String getDomain() {
    String[] parts = address.split("@");
    return parts[parts.length - 1];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof ParticipantId) {
      ParticipantId p = (ParticipantId) o;
      return address.equals(p.address);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return address.hashCode();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return getAddress();
  }
}
