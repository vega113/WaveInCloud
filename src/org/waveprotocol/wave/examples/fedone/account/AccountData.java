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

package org.waveprotocol.wave.examples.fedone.account;


/**
 * Represents a basic immutable account, consists solely out of a username. It
 * has methods to check and convert to other type of accounts.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public interface AccountData {

  /**
   * Gets the username belonging to this account. This is the primary identifier
   * for accounts, very likely to be in a similar form as an email address.
   *
   * @return returns a non-null username.
   */
  String getUsername();

  /**
   * @return true iff this account is a {@link HumanAccountData}.
   */
  boolean isHuman();

  /**
   * Returns this account as a {@link HumanAccountData}.
   *
   * @precondition isHuman()
   */
  HumanAccountData asHuman();

  /**
   * @return true iff this account is a {@link RobotAccountData}.
   */
  boolean isRobot();

  /**
   * Returns this account as a {@link RobotAccountData}.
   *
   * @precondition isRobot()
   */
  RobotAccountData asRobot();
}
