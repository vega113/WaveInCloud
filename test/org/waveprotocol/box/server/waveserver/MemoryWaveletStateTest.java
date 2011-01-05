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

package org.waveprotocol.box.server.waveserver;

import org.waveprotocol.wave.model.id.WaveletName;

import java.util.concurrent.ExecutorService;

/**
 * Runs wavelet state tests with the {@link MemoryWaveletState}.
 *
 * @author anorth@google.com (Alex North)
 */
public class MemoryWaveletStateTest extends WaveletStateTestBase {

  @Override
  protected WaveletState createEmptyState(WaveletName name) {
    return new MemoryWaveletState(name);
  }

  @Override
  protected void awaitPersistence() {
    // All callbacks were already performed in the same thread.
    return;
  }
}
