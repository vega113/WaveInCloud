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

package org.waveprotocol.wave.model.document.operation;

import org.waveprotocol.wave.model.document.operation.util.StateMap;

/**
 * A set of attributes, implemented as a map from attribute names to attribute
 * values. The attributes in the map are sorted by their names, so that the set
 * obtained from entrySet() will allow you to easily iterate through the
 * attributes in sorted order.
 *
 * Implementations must be immutable.
 */
public interface Attributes extends StateMap {
  public Attributes updateWith(AttributesUpdate mutation);
}
