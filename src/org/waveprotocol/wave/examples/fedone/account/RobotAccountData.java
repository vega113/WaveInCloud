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
 * Represents an {@link AccountData} belonging to a Robot.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 *
 */
public interface RobotAccountData extends AccountData {

  /**
   * Returns the URL on which the robot can be located. The URL must not end
   * with /.
   */
  String getUrl();

  // TODO(ljvderijk): Add capabilities
  // /**
  // * Returns the capabilities that this Robot has.
  // */
  // Map<EventType, Capability> getCapabilities();

  /**
   * Returns the hash generated from the capabilities as indicated by the Robot
   * in its capabilities.xml. This may be null if this data has not been
   * retrieved.
   */
  String getCapabilitiesHash();

  /**
   * Returns true iff the robot ownership has been verified and is ready to be
   * used in the Robot API.
   */
  boolean isVerified();
}
