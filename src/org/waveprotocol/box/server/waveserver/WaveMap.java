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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.inject.Inject;

import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A collection of wavelets, local and remote, held in memory.
 *
 * @author soren@google.com (Soren Lassen)
 */
public class WaveMap implements SearchProvider {

  private static final Log LOG = Log.get(WaveMap.class);

  /**
   * The wavelets in a wave.
   */
  private static final class Wave implements Iterable<WaveletContainer> {
    private class WaveletCreator<T extends WaveletContainer> implements Function<WaveletId, T> {
      private final WaveletContainer.Factory<T> factory;

      public WaveletCreator(WaveletContainer.Factory<T> factory) {
        this.factory = factory;
      }

      @Override
      public T apply(WaveletId waveletId) {
        return factory.create(notifiee, WaveletName.of(waveId, waveletId));
      }
    }

    private final WaveId waveId;
    /** Future providing already-existing wavelets in storage. */
    private final ListenableFuture<ImmutableSet<WaveletId>> lookedupWavelets;
    private final ConcurrentMap<WaveletId, LocalWaveletContainer> localWavelets;
    private final ConcurrentMap<WaveletId, RemoteWaveletContainer> remoteWavelets;
    private final WaveletNotificationSubscriber notifiee;

    /**
     * Creates a wave. The {@code lookupWavelets} future is examined only when a
     * query is first made.
     */
    public Wave(WaveId waveId,
        ListenableFuture<ImmutableSet<WaveletId>> lookedupWavelets,
        WaveletNotificationSubscriber notifiee, LocalWaveletContainer.Factory localFactory,
        RemoteWaveletContainer.Factory remoteFactory) {
      this.waveId = waveId;
      this.lookedupWavelets = lookedupWavelets;
      this.notifiee = notifiee;
      this.localWavelets = new MapMaker().makeComputingMap(
          new WaveletCreator<LocalWaveletContainer>(localFactory));
      this.remoteWavelets = new MapMaker().makeComputingMap(
          new WaveletCreator<RemoteWaveletContainer>(remoteFactory));
    }

    @Override
    public Iterator<WaveletContainer> iterator() {
      return Iterators.unmodifiableIterator(
          Iterables.concat(localWavelets.values(), remoteWavelets.values()).iterator());
    }

    public LocalWaveletContainer getLocalWavelet(WaveletId waveletId)
        throws WaveletStateException {
      return getWavelet(waveletId, localWavelets);
    }

    public RemoteWaveletContainer getRemoteWavelet(WaveletId waveletId)
        throws WaveletStateException {
      return getWavelet(waveletId, remoteWavelets);
    }

    public LocalWaveletContainer getOrCreateLocalWavelet(WaveletId waveletId) {
      return localWavelets.get(waveletId);
    }

    public RemoteWaveletContainer getOrCreateRemoteWavelet(WaveletId waveletId) {
      return remoteWavelets.get(waveletId);
    }

    private <T extends WaveletContainer> T getWavelet(WaveletId waveletId,
        ConcurrentMap<WaveletId, T> waveletsMap) throws WaveletStateException {
      ImmutableSet<WaveletId> storedWavelets;
      try {
        storedWavelets =
            FutureUtil.getResultOrPropagateException(lookedupWavelets, PersistenceException.class);
      } catch (PersistenceException e) {
        throw new WaveletStateException(
            "Failed to lookup wavelet " + WaveletName.of(waveId, waveletId), e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new WaveletStateException(
            "Interrupted looking up wavelet " + WaveletName.of(waveId, waveletId), e);
      }
      // Since waveletsMap is a computing map, we must call containsKey(waveletId)
      // to tell if waveletId is mapped, we cannot test if get(waveletId) returns null.
      if (!storedWavelets.contains(waveletId) && !waveletsMap.containsKey(waveletId)) {
        return null;
      } else {
        T wavelet = waveletsMap.get(waveletId);
        Preconditions.checkNotNull(wavelet, "computingMap returned null");
        return wavelet;
      }
    }
  }

  /**
   * Returns a future whose result is the ids of stored wavelets in the given wave.
   * Any failure is reported as a {@link PersistenceException}.
   */
  private static ListenableFuture<ImmutableSet<WaveletId>> lookupWavelets(
      final WaveId waveId, final WaveletStore<?> waveletStore, Executor lookupExecutor) {
    ListenableFutureTask<ImmutableSet<WaveletId>> task =
        new ListenableFutureTask<ImmutableSet<WaveletId>>(
            new Callable<ImmutableSet<WaveletId>>() {
              @Override
              public ImmutableSet<WaveletId> call() throws PersistenceException {
                return waveletStore.lookup(waveId);
              }
            });
    lookupExecutor.execute(task);
    return task;
  }

  private final ConcurrentMap<WaveId, Wave> waves;
  private final WaveletStore<?> store;

  @Inject
  public WaveMap(final DeltaAndSnapshotStore waveletStore,
      final WaveletNotificationSubscriber notifiee,
      final LocalWaveletContainer.Factory localFactory,
      final RemoteWaveletContainer.Factory remoteFactory) {
    // NOTE(anorth): DeltaAndSnapshotStore is more specific than necessary, but
    // helps Guice out.
    // TODO(soren): inject a proper executor (with a pool of configurable size)
    this.store = waveletStore;
    final Executor lookupExecutor = Executors.newSingleThreadExecutor();
    waves = new MapMaker().makeComputingMap(
        new Function<WaveId, Wave>() {
          @Override
          public Wave apply(WaveId waveId) {
            ListenableFuture<ImmutableSet<WaveletId>> lookedupWavelets =
                lookupWavelets(waveId, waveletStore, lookupExecutor);
            return new Wave(waveId, lookedupWavelets, notifiee, localFactory, remoteFactory);
          }
        });
  }

  /**
   * Loads all wavelets from storage.
   *
   * @throws WaveletStateException if storage access fails.
   */
  public void loadAllWavelets() throws WaveletStateException {
    try {
      ExceptionalIterator<WaveId, PersistenceException> itr = store.getWaveIdIterator();
      while (itr.hasNext()) {
        WaveId waveId = itr.next();
        lookupWavelets(waveId);
      }
    } catch (PersistenceException e) {
      throw new WaveletStateException("Failed to scan waves", e);
    }
  }

  @Override
  public Collection<WaveViewData> search(ParticipantId user, String query, int startAt,
      int numResults) {
    int endAt = startAt + numResults - 1;
    LOG.info(
        "Search query '" + query + "' from user: " + user + " [" + startAt + ", " + endAt + "]");
    if (!query.equals("in:inbox") && !query.equals("with:me")) {
      throw new AssertionError("Only queries for the inbox work");
    }
    // Must use a map with stable ordering, since indices are meaningful.
    Map<WaveId, WaveViewData> results = Maps.newLinkedHashMap();
    int resultIndex = 0;
    for (Map.Entry<WaveId, Wave> entry : waves.entrySet()) {
      WaveId waveId = entry.getKey();
      Wave wave = entry.getValue();
      WaveViewData view = null;  // Copy of the wave built up for search hits.
      for (WaveletContainer c : wave) {
        try {
          if (c.hasParticipant(user)) {
            if (view != null) {
              // Just keep adding all the relevant wavelets in this wave.
              view.addWavelet(c.copyWaveletData());
              continue;
            }

            // This wave is in this user's index.
            if (startAt <= resultIndex && resultIndex <= endAt) {
              // ... and it is in the search range, so put it in the results.
              view = WaveViewDataImpl.create(waveId);
              view.addWavelet(c.copyWaveletData());
              results.put(waveId, view);
              resultIndex++;
            } else {
              // ... but not in the search range, so move on to the next wave.
              resultIndex++;
              break;
            }
          }
        } catch (WaveletStateException e) {
          LOG.info("Failed to access wavelet " + c.getWaveletName(), e);
        }
      }
      if (resultIndex > endAt) {
        break;
      }
    }
    LOG.info("Search response to '" + query + "': " + results.size() + " results");
    return results.values();
  }

  public ExceptionalIterator<WaveId, WaveServerException> getWaveIds() {
    Iterator<WaveId> inner = waves.keySet().iterator();
    return ExceptionalIterator.FromIterator.create(inner);
  }

  public ImmutableSet<WaveletId> lookupWavelets(WaveId waveId) throws WaveletStateException {
    ListenableFuture<ImmutableSet<WaveletId>> future = waves.get(waveId).lookedupWavelets;
    try {
      return FutureUtil.getResultOrPropagateException(future, PersistenceException.class);
    } catch (PersistenceException e) {
      throw new WaveletStateException("Failed to look up wave " + waveId, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new WaveletStateException("Interrupted while looking up wave " + waveId, e);
    }
  }

  public LocalWaveletContainer getLocalWavelet(WaveletName waveletName)
      throws WaveletStateException {
    return waves.get(waveletName.waveId).getLocalWavelet(waveletName.waveletId);
  }

  public RemoteWaveletContainer getRemoteWavelet(WaveletName waveletName)
      throws WaveletStateException {
    return waves.get(waveletName.waveId).getRemoteWavelet(waveletName.waveletId);
  }

  public LocalWaveletContainer getOrCreateLocalWavelet(WaveletName waveletName) {
    return waves.get(waveletName.waveId).getOrCreateLocalWavelet(waveletName.waveletId);
  }

  public RemoteWaveletContainer getOrCreateRemoteWavelet(WaveletName waveletName) {
    return waves.get(waveletName.waveId).getOrCreateRemoteWavelet(waveletName.waveletId);
  }
}
