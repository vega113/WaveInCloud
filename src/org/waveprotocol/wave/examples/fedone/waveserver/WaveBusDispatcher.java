// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.examples.fedone.waveserver;

import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

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
  public void waveletUpdate(CoreWaveletData wavelet, ProtocolHashedVersion resultingVersion,
      List<ProtocolWaveletDelta> deltas) {
    for (WaveBus.Subscriber s : subscribers) {
      s.waveletUpdate(wavelet, resultingVersion, deltas);
    }
  }

  @Override
  public void waveletCommitted(WaveletName waveletName, ProtocolHashedVersion version) {
    for (WaveBus.Subscriber s : subscribers) {
      s.waveletCommitted(waveletName, version);
    }
  }
}
