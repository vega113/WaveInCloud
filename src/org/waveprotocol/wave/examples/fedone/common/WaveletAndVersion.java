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
package org.waveprotocol.wave.examples.fedone.common;

import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

/**
 * Class for storing Wavelet snapshots which is a combination of
 * {@link CoreWaveletData} and history hash for that version.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public final class WaveletAndVersion {

  private final CoreWaveletData waveletData;
  private final HashedVersion hashedVersion;

  /**
   * Constructs a snapshot of a Wavelet. The caller is responsible for providing
   * the right history hash.
   *
   * @param waveletData the waveletData to store in this snapshot.
   * @param hashedVersion the {@link HashedVersion} matching the given Wavelet
   *        data.
   */
  public WaveletAndVersion(CoreWaveletData waveletData, HashedVersion hashedVersion) {
    this.waveletData = waveletData;
    this.hashedVersion = hashedVersion;
  }

  /**
   * @return the waveletData stored in this snapshot.
   */
  public CoreWaveletData getWaveletData() {
    return waveletData;
  }

  /**
   * @return the historyHash for the {@link CoreWaveletData} stored in this
   *         snapshot.
   */
  public HashedVersion getHashedVersion() {
    return hashedVersion;
  }
}
