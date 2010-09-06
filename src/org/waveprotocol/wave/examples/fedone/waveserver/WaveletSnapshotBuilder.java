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

package org.waveprotocol.wave.examples.fedone.waveserver;

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.model.wave.data.WaveletData;

/**
 * A builder class. Passed in to WaveletProvider.getSnapshot to allow the snapshot and versions
 * to be retrieved atomicly.
 *
 * @author arb@google.com (Anthony Baxter)
 * @param <T> The type that the builder creates.
 */
public interface WaveletSnapshotBuilder<T> {

  /**
   * Build a snapshot.
   */
  T build(WaveletData waveletData, HashedVersion currentVersion,
      ProtocolHashedVersion committedVersion);
}
