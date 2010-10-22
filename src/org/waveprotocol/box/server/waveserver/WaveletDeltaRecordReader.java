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

import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.operation.core.TransformedWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.io.IOException;
import java.util.Collection;

/**
 * Reads wavelet deltas for a wavelet.
 *
 * @author soren@google.com (Soren Lassen)
 */
public interface WaveletDeltaRecordReader {

  /** @return the name of the wavelet whose deltas this reader reads */
  WaveletName getWaveletName();

  /** @return the resulting version of the last delta */
  HashedVersion getEndVersion();

  /** @return the delta applied at this version, if any, otherwise null */
  WaveletDeltaRecord getDelta(long version) throws IOException;

  /**
   * Convenience method, approximately equivalent to
   * {@code AppliedDeltaUtil.getHashedVersionAppliedAt(getAppliedDelta(version))}
   *
   * @return the hashed version for the specified version number, if any delta
   *         was applied at this version, otherwise null
   */
  HashedVersion getAppliedAtVersion(long version) throws IOException;

  /**
   * Convenience method, approximately equivalent to
   * {@code getTransformedDelta(version).getResultingVersion()}
   *
   * @return the resulting hashed version of the delta applied at the given
   *         version, if any, otherwise null
   */
  HashedVersion getResultingVersion(long version) throws IOException;

  /**
   * Convenience method, approximately equivalent to
   * {@code getDelta(version).applied)}
   *
   * @return the applied delta applied at this version, if any, otherwise null
   */
  ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDelta(long version) throws IOException;

  /**
   * Convenience method, approximately equivalent to
   * {@code getDelta(version).transformed)}
   *
   * @return the transformed delta applied at this version, if any, otherwise
   *         null
   */
  TransformedWaveletDelta getTransformedDelta(long version) throws IOException;
}
