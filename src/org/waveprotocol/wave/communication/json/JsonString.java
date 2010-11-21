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

package org.waveprotocol.wave.communication.json;

/**
 * This is a wrapper around a java String that contains JSON.
 *
 * The intent is to clarify where JSON has been provided (and is required).
 * Consequently, only a subset of the String properties are supported.
 * JSON is assumed to be UTF-8 safe.
 *
 */
public class JsonString {

  /** The underlying string object */
  private final String json;

  public JsonString(String json) {
    this.json = json;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof JsonString) {
      return json.equals(((JsonString) o).json);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return json.hashCode();
  }

  @Override
  public String toString() {
    return json;
  }
}
