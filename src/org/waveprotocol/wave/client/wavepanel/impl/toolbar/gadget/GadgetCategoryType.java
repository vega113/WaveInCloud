/**
 * Copyright 2011 Google Inc.
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
 */

package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget;

/**
 * @author vega113@gmail.com (Yuri Zelikov)
 */
public enum GadgetCategoryType {
  ALL("All"),
  GAME("Game"),
  IMAGE("Image"),
  MAP("Map"),
  MUSIC("Music"),
  PRODUCTIVITY("Productivity"),
  SEARCH("Search"),
  TEAM("Team"),
  TIME("Time"),
  TRAVEL("Travel"),
  UTILITY("Utility"),
  VIDEO("Video"),
  VOICE("Voice"),
  VOTING("Voting"),
  OTHER("Other"),
  ;
  
  private final String value;
  
  private GadgetCategoryType(String value) {
    this.value = value;
  }
  
  public String getValue() {
    return value;
  }
}
