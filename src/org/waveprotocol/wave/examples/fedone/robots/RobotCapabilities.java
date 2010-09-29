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

package org.waveprotocol.wave.examples.fedone.robots;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.event.EventType;
import com.google.wave.api.robot.Capability;

import java.util.Map;

/**
 * Represents the capabilities that have been retrieved from a robot's
 * capabilities.xml file.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotCapabilities {

  private final Map<EventType, Capability> capabilities;
  private final String capabilitiesHash;
  private final ProtocolVersion version;

  /**
   * Constructs a new {@link RobotCapabilities} object with the given data.
   *
   * @param capabilitiesMap mapping events to capabilities for this robot.
   * @param capabilitiesHash the hash of the robot's capabilities.
   * @param version the {@link ProtocolVersion} the robot speaks.
   */
  public RobotCapabilities(Map<EventType, Capability> capabilitiesMap, String capabilitiesHash,
      ProtocolVersion version) {
    Preconditions.checkNotNull(capabilitiesMap, "Capabilities map may not be null");
    Preconditions.checkNotNull(capabilitiesHash, "Capabilities hash may not be null");
    Preconditions.checkNotNull(version, "Version may not be null");

    this.capabilities = ImmutableMap.copyOf(capabilitiesMap);
    this.capabilitiesHash = capabilitiesHash;
    this.version = version;
  }

  /**
   * Returns the capabilities map of the robot.
   */
  public Map<EventType, Capability> getCapabilitiesMap() {
    return capabilities;
  }

  /**
   * Returns the capabilities hash.
   */
  public String getCapabilitiesHash() {
    return capabilitiesHash;
  }

  /**
   * Returns the {@link ProtocolVersion} that the robot speaks.
   */
  public ProtocolVersion getProtocolVersion() {
    return version;
  }

  @Override
  public int hashCode() {
    return capabilitiesHash.hashCode();
  }

  /**
   * {@link RobotCapabilities} are equal when their capabilities hash matches.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RobotCapabilities)) {
      return false;
    }
    RobotCapabilities other = (RobotCapabilities) obj;
    return capabilitiesHash.equals(other.capabilitiesHash);
  }
}
