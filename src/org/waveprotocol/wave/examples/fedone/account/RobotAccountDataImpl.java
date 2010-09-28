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

import org.waveprotocol.wave.examples.fedone.robots.RobotCapabilities;

/**
 * Robot Account implementation.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public final class RobotAccountDataImpl implements RobotAccountData {

  private final String address;
  private final String url;
  private final RobotCapabilities capabilities;
  private final boolean isVerified;

  /**
   * Creates a new {@link RobotAccountData}.
   *
   *  The capabilities map and version may only be null if the capabilitiesHash
   * is null and vice versa.
   *
   * @param address non-null address for this account.
   * @param url non-null Url where the robot can be reached.
   * @param capabilities {@link RobotCapabilities} representing the robot's
   *        capabilties.xml. May be null.
   * @param isVerified boolean indicating whether this {@link RobotAccountData}
   *        has been verified.
   */
  public RobotAccountDataImpl(
      String address, String url, RobotCapabilities capabilities, boolean isVerified) {
    Preconditions.checkNotNull(address, "Address can not be null");
    Preconditions.checkNotNull(url, "Url can not be null");
    Preconditions.checkArgument(!url.endsWith("/"), "Url must not end with /");

    this.address = address;
    this.url = url;
    this.capabilities = capabilities;
    this.isVerified = isVerified;
  }

  @Override
  public String getAddress() {
    return address;
  }

  @Override
  public boolean isHuman() {
    return false;
  }

  @Override
  public HumanAccountData asHuman() {
    throw new UnsupportedOperationException("Can't turn a RobotAccount into a HumanAccount");
  }

  @Override
  public boolean isRobot() {
    return true;
  }

  @Override
  public RobotAccountData asRobot() {
    return this;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public RobotCapabilities getCapabilities() {
    return capabilities;
  }

  @Override
  public boolean isVerified() {
    return isVerified;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((capabilities == null) ? 0 : capabilities.hashCode());
    result = prime * result + (isVerified ? 1231 : 1237);
    result = prime * result + url.hashCode();
    result = prime * result + address.hashCode();
    return result;
  }

  /**
   * Robots are equal if their username, url, capabilities and verification are
   * equal.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RobotAccountDataImpl)) {
      return false;
    }

    RobotAccountDataImpl other = (RobotAccountDataImpl) obj;

    if (capabilities == null) {
      if (other.capabilities != null) {
        return false;
      }
    }

    return address.equals(other.address) && url.equals(other.url)
           && capabilities.equals(other.capabilities) && isVerified == other.isVerified;
  }
}
