/**
 * Copyright 2009 Google Inc.
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

package org.waveprotocol.wave.examples.fedone.waveclient.common;

import com.google.common.collect.Maps;

import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.common.HashedVersionFactory;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A client's view of a wave, with the current wavelet versions.
 *
 *
 */
public class ClientWaveView {
  /** Wave this is the view of. */
  private final WaveViewData data;

  /** Last known version of each wavelet. */
  private final Map<WaveletId, HashedVersion> currentVersions;

  /** Factory for creating hashed versions. */
  private final HashedVersionFactory hashedVersionFactory;

  /**
   * Read Lock for changes to wavelet versions.
   * TODO: move to ClientWaveletView when it exists
   */
  private final Lock versionReadLock;

  /**
   * Write lock for changes to wavelet versions.
   * TODO: move to ClientWaveletView when it exists
   */
  private final Lock versionWriteLock;

  /**
   * Condition variable for signalling version changes to wavelets
   * TODO: move to ClientWaveletView when it exists
   */
  private final Condition versionChangeCondition;

  /**
   * @param hashedVersionFactory for generating hashed versions
   * @param waveId of the wave
   */
  public ClientWaveView(HashedVersionFactory hashedVersionFactory, WaveId waveId) {
    this.hashedVersionFactory = hashedVersionFactory;
    this.data = new WaveViewDataImpl(waveId);
    this.currentVersions = Maps.newHashMap();
    ReadWriteLock versionReadWriteLock = new ReentrantReadWriteLock();
    versionReadLock = versionReadWriteLock.readLock();
    versionWriteLock = versionReadWriteLock.writeLock();
    versionChangeCondition = versionWriteLock.newCondition();
  }

  /**
   * Get the unique identifier of the wave in view.
   *
   * @return the unique identifier of the wave.
   */
  public WaveId getWaveId() {
    return data.getWaveId();
  }

  /**
   * Gets the wavelets in this wave view. The order of iteration is unspecified.
   *
   * @return wavelets in this wave view.
   */
  public Iterable<? extends WaveletData> getWavelets() {
    return data.getWavelets();
  }

  /**
   * Gets the last known version for a wavelet.
   *
   * @param waveletId of the wavelet
   * @return last known version for wavelet
   */
  public HashedVersion getWaveletVersion(WaveletId waveletId) {
    versionReadLock.lock();
    try {
      HashedVersion version = currentVersions.get(waveletId);
      if (version == null) {
        throw new IllegalArgumentException(waveletId + " is not a wavelet of " + data.getWaveId());
      } else {
        return version;
      }
    } finally {
      versionReadLock.unlock();
    }
  }

  /**
   * Sets the last known version for a wavelet.
   *
   * @param waveletId of the wavelet
   * @param version of the wavelet
   */
  public void setWaveletVersion(WaveletId waveletId, HashedVersion version) {
    versionWriteLock.lock();
    try {
      currentVersions.put(waveletId, version);
      versionChangeCondition.signalAll();
    } finally {
      versionWriteLock.unlock();
    }
  }

  /**
   * Block and wait for a wavelet to reach a given version.  If this wave has no record of the
   * wavelet, wait until it has been created.
   *
   * @param waveletId of the wavelet
   * @param version of the wavelet
   * @param timeout to block for
   * @param unit of timeunit
   * @return false if the waiting time elapsed, true otherwise
   */
  public boolean awaitWaveletVersion(WaveletId waveletId, long version, long timeout,
      TimeUnit unit) {
    versionWriteLock.lock();
    long timeoutNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeout, unit);
    try {
      while ((getWavelet(waveletId) == null)
          || (getWaveletVersion(waveletId).getVersion() < version)) {
        if (timeoutNanos < System.nanoTime()) {
          return false;
        }
        versionChangeCondition.awaitNanos(timeoutNanos - System.nanoTime());
      }
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    } finally {
      versionWriteLock.unlock();
    }
    return true;
  }

  /**
   * Get a wavelet from the view by id.
   *
   * @return the requested wavelet, or null if it is not in view.
   */
  public WaveletData getWavelet(WaveletId waveletId) {
    return data.getWavelet(waveletId);
  }

  /**
   * Create a wavelet in the wave.
   *
   * @param waveletId of new wavelet, which must be unique within the wave
   * @return wavelet created
   */
  public WaveletData createWavelet(WaveletId waveletId) {
    WaveletName name = WaveletName.of(data.getWaveId(), waveletId);
    WaveletData wavelet = data.createWavelet(waveletId);
    currentVersions.put(waveletId, hashedVersionFactory.createVersionZero(name));
    return wavelet;
  }

  /**
   * Removes a wavelet and its current hashed version from the wave view.
   *
   * @param waveletId of wavelet to remove
   */
  public void removeWavelet(WaveletId waveletId) {
    data.removeWavelet(waveletId);
    currentVersions.remove(waveletId);
  }
}
