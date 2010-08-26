// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.examples.fedone.waveserver;

import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.id.WaveletName;

import java.util.List;
import java.util.Map;

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
     * TODO(anorth): replace documentState map with the wavelet data
     *
     * @param waveletName wavelet to which deltas apply
     * @param deltas deltas applied to the wavelet
     * @param resultingVersion version of the wavelet after deltas
     * @param documentState the state of each document (by documentId) in the
     *        wavelet after the deltas have been applied.
     */
    void waveletUpdate(WaveletName waveletName, List<ProtocolWaveletDelta> deltas,
        ProtocolHashedVersion resultingVersion, Map<String, BufferedDocOp> documentState);

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
