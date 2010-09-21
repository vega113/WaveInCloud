/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.wave.examples.fedone.common;

import org.waveprotocol.wave.examples.common.HashedVersion;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;

/**
 * A wavelet delta with a target hashed version.
 */
public final class VersionedWaveletDelta {

  public final CoreWaveletDelta delta;
  public final HashedVersion version;

  public VersionedWaveletDelta(CoreWaveletDelta delta, HashedVersion version) {
    this.delta = delta;
    this.version = version;
  }

  @Override
  public int hashCode() {
    return delta.hashCode() * 31 + version.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof VersionedWaveletDelta)) {
      return false;
    } else {
      VersionedWaveletDelta that = (VersionedWaveletDelta) obj;
      return delta.equals(that.delta) && version.equals(that.version);
    }
  }

  @Override
  public String toString() {
    return delta + "@" + version;
  }
}
