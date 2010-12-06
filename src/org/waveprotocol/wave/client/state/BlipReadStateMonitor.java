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
 */
package org.waveprotocol.wave.client.state;


/**
 * Monitors the conversation for unread blips.
 */
public interface BlipReadStateMonitor {

  /**
   * Listener interface for changes to read/unread blip counts.
   */
  interface Listener {
    /**
     * Called when the read/unread count changes.  This event will also fire
     * when the monitor becomes ready, with the initial read/unread count.
     */
    void onReadStateChanged(int readCount, int unreadCount);
  }

  /**
   * @return The current read blip count, or 0 if the monitor isn't ready.
   */
  int getReadCount();

  /**
   * @return The current unread blip count, or 0 if the monitor isn't ready.
   */
  int getUnreadCount();

  /**
   * @return Whether the read/unread state is ready to be queried.
   */
  boolean isReady();

  /**
   * Adds a listener to change events.
   */
  void addListener(Listener listener);

  /**
   * Removes a listener to change events.
   */
  void removeListener(Listener listener);
}
