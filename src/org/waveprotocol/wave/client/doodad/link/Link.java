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

package org.waveprotocol.wave.client.doodad.link;

/**
 * Annotation keys for links.
 *
 */
public final class Link {
  /** Key prefix */
  public static final String PREFIX = "link";
  /** Key for 'linky' agent created links. */
  public static final String AUTO_KEY = PREFIX + "/auto";
  /** Key for manually created links. */
  public static final String MANUAL_KEY = PREFIX + "/manual";
  /** Key for wave links. */
  public static final String WAVE_KEY = PREFIX + "/wave";
  /**
   * Array of all link keys
   *
   * NOTE(user): Code may implicitly depend on the order of these keys. We
   * should expose LINK_KEYS as a set, and have ORDERED_LINK_KEYS for code that
   * relies on specific ordering.
   */
  public static final String[] LINK_KEYS = {AUTO_KEY, MANUAL_KEY, WAVE_KEY};

  private Link() {
  }

  /**
   * @param key
   * @return iff the given key is a link key.
   */
  public static boolean isLinkKey(String key) {
    for (String linkKey : LINK_KEYS) {
      if (linkKey.equals(key)) {
        return true;
      }
    }
    return false;
  }
}
