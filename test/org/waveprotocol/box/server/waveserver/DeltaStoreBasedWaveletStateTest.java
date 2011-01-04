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

import org.waveprotocol.box.server.persistence.memory.MemoryDeltaStore;
import org.waveprotocol.box.server.waveserver.DeltaStore;

import org.waveprotocol.wave.model.id.WaveletName;

/**
 * Runs wavelet state tests with the {@link DeltaStoreBasedWaveletState}.
 *
 * @author soren@google.com (Soren Lassen)
 */
public class DeltaStoreBasedWaveletStateTest extends WaveletStateTestBase {

  private DeltaStore store;

  @Override
  public void setUp() throws Exception {
    store = new MemoryDeltaStore();
    super.setUp();
  }

  @Override
  protected WaveletState createEmptyState(WaveletName name) throws Exception {
    return DeltaStoreBasedWaveletState.create(store.open(name));
  }

  @Override
  protected void awaitPersistence() throws Exception {
    Thread.sleep(1); // TODO(soren): this is bad, rather inject persistExecutor and control it here
    return;
  }

  // TODO(soren): We need to add tests here that verify interactions with storage.
  // The base tests only test the public interface, not any interactions with the storage system.
}
