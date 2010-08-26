// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.examples.fedone.waveserver;

import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;

import java.util.List;
import java.util.Map;

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
  public void waveletUpdate(WaveletName waveletName, List<ProtocolWaveletDelta> newDeltas,
      ProtocolHashedVersion resultingVersion, Map<String, BufferedDocOp> documentState) {
    for (WaveBus.Subscriber s : subscribers) {
      s.waveletUpdate(waveletName, newDeltas, resultingVersion, documentState);
    }
  }

  @Override
  public void waveletCommitted(WaveletName waveletName, ProtocolHashedVersion version) {
    for (WaveBus.Subscriber s : subscribers) {
      s.waveletCommitted(waveletName, version);
    }
  }
}
