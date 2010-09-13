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

package org.waveprotocol.wave.examples.fedone.frontend;


import com.google.common.base.Preconditions;

import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.WaveletSnapshot;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;

/**
 * A wavelet snapshot with committed version.
 *
 * @author arb@google.com (Anthony Baxter)
*/
public final class WaveletSnapshotAndVersion {
  public final WaveletSnapshot snapshot;
  public final ProtocolHashedVersion committedVersion;

  public WaveletSnapshotAndVersion(WaveletSnapshot snapshot,
      ProtocolHashedVersion committedVersion) {
    Preconditions.checkNotNull(snapshot);
    this.snapshot = snapshot;
    this.committedVersion = committedVersion;
  }
}
