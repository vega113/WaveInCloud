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

package org.waveprotocol.wave.examples.webclient.common.communication.callback;

import org.waveprotocol.wave.examples.webclient.common.communication.interchange.StatusCode;


/**
 * Specialisation of SimpleCallback for non-streaming remote calls. The failure
 * type is an interchange status code.
 *
 * @param <R> type of a successful response.
 * @author anorth@google.com (Alex North)
 */
public interface RemoteCallback<R> extends SimpleCallback<R, StatusCode> {
}
