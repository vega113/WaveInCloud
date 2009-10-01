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
 * A document initialization.
 *
 * Document initializations are document operations that apply to the empty
 * document; they do not contain update or deletion components.
 *
 * This interface only offers a visitor pattern ('apply') to enumerate the
 * components; the data structure behind the operation remains opaque.
 */
public interface DocInitialization extends DocOp {

  void apply(DocInitializationCursor c);

}
