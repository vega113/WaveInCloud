// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.examples.fedone.waveserver;

import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.List;

/**
 * Provides a subscription service for changes to wavelets.
 *
 * @author anorth@google.com (Alex North)
 */
public interface WaveBus {
  /**
   * Receives wave bus messages.
   */
  interface Subscriber {
    /**
     * Notifies the subscriber of a wavelet update.
     *
     * TODO(anorth): replace WaveletData with a read-only wavelet data.
     *
     * @param wavelet the state of the wavelet wavelet after the deltas have
     *        been applied.
     * @param resultingVersion version of the wavelet after deltas
     * @param deltas deltas applied to the wavelet
     */
    void waveletUpdate(WaveletData wavelet, ProtocolHashedVersion resultingVersion,
        List<ProtocolWaveletDelta> deltas);

    /**
     * Notifies the subscriber that a wavelet has been committed to persistent
     * storage.
     *
     * @param waveletName name of wavelet
     * @param version the version and hash of the wavelet as it was committed
     */
    void waveletCommitted(WaveletName waveletName, ProtocolHashedVersion version);
  }

  /**
   * Subscribes to the bus, if the subscriber is not already subscribed.
   */
  void subscribe(Subscriber s);

  /**
   * Unsubscribes from the bus, if the subscriber is currently subscribed.
   */
  void unsubscribe(Subscriber s);
}
