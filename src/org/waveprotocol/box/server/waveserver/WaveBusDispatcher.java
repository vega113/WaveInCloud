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
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.util.List;

/**
 * Dispatches messages to a collection of wave bus subscribers.
 *
 * @author anorth@google.com (Alex North)
 */
public final class WaveBusDispatcher implements WaveBus, WaveBus.Subscriber {

  private final CopyOnWriteSet<WaveBus.Subscriber> subscribers = CopyOnWriteSet.createListSet();

  @Override
  public void subscribe(Subscriber s) {
    subscribers.add(s);
  }

  @Override
  public void unsubscribe(Subscriber s) {
    subscribers.remove(s);
  }

  @Override
  public void waveletUpdate(ReadableWaveletData wavelet, HashedVersion resultingVersion,
      List<CoreWaveletDelta> deltas) {
    for (WaveBus.Subscriber s : subscribers) {
      s.waveletUpdate(wavelet, resultingVersion, deltas);
    }
  }

  @Override
  public void waveletCommitted(WaveletName waveletName, HashedVersion version) {
    for (WaveBus.Subscriber s : subscribers) {
      s.waveletCommitted(waveletName, version);
    }
  }
}
