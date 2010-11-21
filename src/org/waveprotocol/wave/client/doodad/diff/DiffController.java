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

package org.waveprotocol.wave.client.doodad.diff;


/**
 * The diff control is used to control an object that can be used to show diffs.
 *
 */
// TODO(danilatos): This belongs in a more common package.
public interface DiffController {

  /**
   * Sets or clears highlight diff mode.
   *
   * @param isOn
   */
  void setShowDiffMode(boolean isOn);

  /**
   * Clears the diffs shown.
   */
  void clearDiffs();

  DiffController NOOP_IMPL = new DiffController() {
    @Override
    public void clearDiffs() {
    }

    @Override
    public void setShowDiffMode(boolean isOn) {
    }
  };
}
