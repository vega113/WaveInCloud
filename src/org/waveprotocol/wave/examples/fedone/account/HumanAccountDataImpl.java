/**
 * Copyright 2010 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.waveprotocol.wave.examples.fedone.account;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.examples.fedone.authentication.PasswordDigest;

/**
 * Human Account. Expected to be expanded when authentication is implemented.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public final class HumanAccountDataImpl implements HumanAccountData {
  private final String address;
  private final PasswordDigest passwordDigest;

  /**
   * Creates an {@link HumanAccountData} for the given username, with no
   * password.
   *
   * This user will not be able to login using password-bsed authentication.
   *
   * @param address non-null username for this account.
   */
  public HumanAccountDataImpl(String address) {
    this(address, null);
  }

  /**
   * Creates an {@link HumanAccountData} for the given username.
   *
   * @param address non-null username for this account.
   * @param password The user's password, or null if the user should not be
   *        authenticated using a password.
   */
  public HumanAccountDataImpl(String address, char[] password) {
    Preconditions.checkNotNull(address, "Username can not be null");

    this.address = address;
    passwordDigest = (password == null) ? null : new PasswordDigest(password);
  }

  @Override
  public String getAddress() {
    return address;
  }

  @Override
  public PasswordDigest getPasswordDigest() {
    return passwordDigest;
  }

  @Override
  public boolean isHuman() {
    return true;
  }

  @Override
  public HumanAccountData asHuman() {
    return this;
  }

  @Override
  public boolean isRobot() {
    return false;
  }

  @Override
  public RobotAccountData asRobot() {
    throw new UnsupportedOperationException("Can't turn a HumanAccount into a RobotAccount");
  }

  @Override
  public int hashCode() {
    return address.hashCode();
  }

  /**
   * An {@link HumanAccountDataImpl} is equal when the usernames match.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof HumanAccountDataImpl)) {
      return false;
    }
    HumanAccountDataImpl other = (HumanAccountDataImpl) obj;
    return address.equals(other.address);
  }
}
