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

/**
 * A DocInitialization whose components are in a random-access buffer.
 *
 * Implementations MUST be immutable.
 */
public interface BufferedDocInitialization
    extends DocInitialization, BufferedDocOp {

  void applyComponent(int i, DocInitializationCursor c);
  DocInitializationComponentType getType(int i);

}
