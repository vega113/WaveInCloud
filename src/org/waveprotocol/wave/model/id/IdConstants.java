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

package org.waveprotocol.wave.model.id;

/**
 * Constants useful for the context of ids.
 *
 *
 */
public interface IdConstants {
  /** Conventional separator for tokens in an id. */
  public static final char TOKEN_SEPARATOR = '+';

  /** Scheme for wave and wavelet URIs. */
  public static final String WAVE_URI_SCHEME = "wave";

  /** Prefix for conversational wave ids. */
  public static final String WAVE_PREFIX = "w";

  /** Prefix for profile wave ids. */
  public static final String PROFILE_WAVE_PREFIX = "prof";

  /** Prefix for conversational wavelet ids. */
  public static final String CONVERSATION_WAVELET_PREFIX = "conv";

  /** Prefix for user-data wavelet ids. */
  public static final String USER_DATA_WAVELET_PREFIX = "user";

  /** Prefix for blip document ids. */
  public static final String BLIP_PREFIX = "b";

  /** Name of the data document that contains tags information. */
  public static final String TAGS_DOC_ID = "tags";

  /** Prefix for ghost blip document ids. Ghost blips aren't rendered. */
  public static final String GHOST_BLIP_PREFIX = "g";

  /** Conventional conversation root wavelet id. */
  public static final String CONVERSATION_ROOT_WAVELET =
      CONVERSATION_WAVELET_PREFIX + TOKEN_SEPARATOR + "root";

  /** Prefix of the name of the attachment metadata data document. */
  public static final String ATTACHMENT_METADATA_PREFIX = "attach";

  /** Old metadata document id. TODO: remove. */
  public static final String OLD_METADATA_DOC_ID = "m/metadata";
}
