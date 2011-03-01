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
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.inject.Inject;

import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.impl.WaveViewDataImpl;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  
  /**
   * Helper class that allows to add basic sort and filter functionality to the
   * search.
   * 
   * @author vega113@gmail.com (Yuri Z.)
   */
  private static class QueryHelper {
    
    @SuppressWarnings("serial")
    class InvalidQueryException extends Exception {

      public InvalidQueryException(String msg) {
        super(msg);
      }
    }
    
    /**
     * Unknown participantId used by {@link ASC_CREATOR_COMPARATOR} in case wave
     * creator cannot be found.
     */
    static final ParticipantId UNKNOWN_CREATOR = ParticipantId.ofUnsafe("unknown@example.com");

    /** Sorts search result in ascending order by LMT. */
    static final Comparator<WaveViewData> ASC_LMT_COMPARATOR = new Comparator<WaveViewData>() {
      @Override
      public int compare(WaveViewData arg0, WaveViewData arg1) {
        long lmt0 = computeLmt(arg0);
        long lmt1 = computeLmt(arg1);
        return Long.signum(lmt0 - lmt1);
      }

      private long computeLmt(WaveViewData wave) {
        long lmt = -1;
        for (ObservableWaveletData wavelet : wave.getWavelets()) {
          // Skip non conversational wavelets.
          if (!IdUtil.isConversationalId(wavelet.getWaveletId())) {
            continue;
          }
          lmt = lmt < wavelet.getLastModifiedTime() ? wavelet.getLastModifiedTime() : lmt;
        }
        return lmt;
      }
    };

    /** Sorts search result in descending order by LMT. */
    static final Comparator<WaveViewData> DESC_LMT_COMPARATOR = new Comparator<WaveViewData>() {
      @Override
      public int compare(WaveViewData arg0, WaveViewData arg1) {
        return -ASC_LMT_COMPARATOR.compare(arg0, arg1);
      }
    };

    /** Sorts search result in ascending order by creation time. */
    static final Comparator<WaveViewData> ASC_CREATED_COMPARATOR = new Comparator<WaveViewData>() {
      @Override
      public int compare(WaveViewData arg0, WaveViewData arg1) {
        long time0 = computeCreatedTime(arg0);
        long time1 = computeCreatedTime(arg1);
        return Long.signum(time0 - time1);
      }

      private long computeCreatedTime(WaveViewData wave) {
        long creationTime = -1;
        for (ObservableWaveletData wavelet : wave.getWavelets()) {
          creationTime =
              creationTime < wavelet.getCreationTime() ? wavelet.getCreationTime() : creationTime;
        }
        return creationTime;
      }
    };

    /** Sorts search result in descending order by creation time. */
    static final Comparator<WaveViewData> DESC_CREATED_COMPARATOR = new Comparator<WaveViewData>() {
      @Override
      public int compare(WaveViewData arg0, WaveViewData arg1) {
        return -ASC_CREATED_COMPARATOR.compare(arg0, arg1);
      }
    };

    /** Sorts search result in ascending order by creator */
    static final Comparator<WaveViewData> ASC_CREATOR_COMPARATOR = new Comparator<WaveViewData>() {
      @Override
      public int compare(WaveViewData arg0, WaveViewData arg1) {
        ParticipantId creator0 = computeCreator(arg0);
        ParticipantId creator1 = computeCreator(arg1);
        return creator0.compareTo(creator1);
      }

      private ParticipantId computeCreator(WaveViewData wave) {
        for (ObservableWaveletData wavelet : wave.getWavelets()) {
          if (IdUtil.isConversationRootWaveletId(wavelet.getWaveletId())) {
            return wavelet.getCreator();
          }
        }
        // If not found creator - compare with UNKNOWN_CREATOR;
        return UNKNOWN_CREATOR;
      }
    };

    /** Sorts search result in descending order by creator */
    static final Comparator<WaveViewData> DESC_CREATOR_COMPARATOR = new Comparator<WaveViewData>() {
      @Override
      public int compare(WaveViewData arg0, WaveViewData arg1) {
        return -ASC_CREATOR_COMPARATOR.compare(arg0, arg1);
      }
    };

    /** Sorts search result by WaveId. */
    static final Comparator<WaveViewData> ID_COMPARATOR = new Comparator<WaveViewData>() {
      @Override
      public int compare(WaveViewData arg0, WaveViewData arg1) {
        return arg0.getWaveId().compareTo(arg1.getWaveId());
      }
    };

    /**
     * Orders using {@link ASCENDING_DATE_COMPARATOR}.
     */
    static final Ordering<WaveViewData> ASC_LMT_ORDERING = Ordering
        .from(QueryHelper.ASC_LMT_COMPARATOR);

    /**
     * Orders using {@link DESCENDING_DATE_COMPARATOR}.
     */
    static final Ordering<WaveViewData> DESC_LMT_ORDERING = Ordering
        .from(QueryHelper.DESC_LMT_COMPARATOR);

    /**
     * Orders using {@link ASC_CREATED_COMPARATOR}.
     */
    static final Ordering<WaveViewData> ASC_CREATED_ORDERING = Ordering
        .from(QueryHelper.ASC_CREATED_COMPARATOR);

    /**
     * Orders using {@link DESC_CREATED_COMPARATOR}.
     */
    static final Ordering<WaveViewData> DESC_CREATED_ORDERING = Ordering
        .from(QueryHelper.DESC_CREATED_COMPARATOR);

    /**
     * Orders using {@link ASC_CREATOR_COMPARATOR}.
     */
    static final Ordering<WaveViewData> ASC_CREATOR_ORDERING = Ordering
        .from(QueryHelper.ASC_CREATOR_COMPARATOR);

    /**
     * Orders using {@link DESC_CREATOR_COMPARATOR}.
     */
    static final Ordering<WaveViewData> DESC_CREATOR_ORDERING = Ordering
        .from(QueryHelper.DESC_CREATOR_COMPARATOR);

    /** Default ordering is by LMT descending. */
    static final Ordering<WaveViewData> DEFAULT_ORDERING = DESC_LMT_ORDERING;
    
    /** Valid search query types. */
    enum TokenQueryType {
      IN("in"),
      ORDERBY("orderby"),
      WITH("with"),
      CREATOR("creator");
      
      final String token;
      
      TokenQueryType(String token) {
        this.token = token;
      }
      
      String getToken() {
        return token;
      }
      
      private static final Map<String, TokenQueryType> reverseLookupMap = 
        new HashMap<String, TokenQueryType>();
      static {
        for (TokenQueryType type : TokenQueryType.values()) {
          reverseLookupMap.put(type.getToken(), type);
        }
      }
      
      static TokenQueryType fromToken(String token) {
        TokenQueryType qyeryToken = reverseLookupMap.get(token);
        if (qyeryToken == null) {
          throw new IllegalArgumentException("Illegal query param: " + token);
        }
        return reverseLookupMap.get(token);
      }
      
      static boolean hasToken(String token) {
        return reverseLookupMap.keySet().contains(token);
      }
    }
    
    /** Registered order by parameter types and corresponding orderings. */
    enum OrderByValueType {
      DATEASC("dateasc", ASC_LMT_ORDERING),
      DATEDESC("datedesc", DESC_LMT_ORDERING),
      CREATEDASC("createdasc", ASC_CREATED_ORDERING),
      CREATEDDESC("createddesc", DESC_CREATED_ORDERING),
      CREATORASC("creatorasc", ASC_CREATOR_ORDERING),
      CREATORDESC("creatordesc", DESC_CREATOR_ORDERING);
      
      final String value;
      final Ordering<WaveViewData> ordering;
      
      OrderByValueType(String value, Ordering<WaveViewData> ordering) {
        this.value = value;
        this.ordering = ordering;
      }
      
      String getToken() {
        return value;
      }
      
      Ordering<WaveViewData> getOrdering() {
        return ordering;
      }
      
      private static final Map<String, OrderByValueType> reverseLookupMap = 
        new HashMap<String, OrderByValueType>();
      
      static {
        for (OrderByValueType type : OrderByValueType.values()) {
          reverseLookupMap.put(type.getToken(), type);
        }
      }
      
      static OrderByValueType fromToken(String token) {
        OrderByValueType orderByValue = reverseLookupMap.get(token);
        if (orderByValue == null) {
          throw new IllegalArgumentException("Illegal 'orderby' value: " + token);
        }
        return reverseLookupMap.get(token);
      }
    }
    
    private QueryHelper() {
      
    }
    
    /** Static factory method. */
    static QueryHelper newQueryHelper() {
      return new QueryHelper();
    }
    
    /**
     * Parses the search query.
     * 
     * @param query the query.
     * @return the result map with query tokens. Never returns null.
     * @throws InvalidQueryException if the query contains invalid params.
     */
    Map<TokenQueryType, Set<String>> parseQuery(String query) throws InvalidQueryException {
      Preconditions.checkArgument(query != null);
      String[] tokens = query.split(" ");
      Map<TokenQueryType, Set<String>> tokensMap = Maps.newEnumMap(TokenQueryType.class);
      for (String token : tokens) {
        String[] pair = token.split(":");
        String tokenValue = pair[1];
        if (pair.length != 2 || !TokenQueryType.hasToken(pair[0])) {
          String msg = "Invalid query param: " + token;
          throw new InvalidQueryException(msg);
        }
        TokenQueryType tokenType = TokenQueryType.fromToken(pair[0]);
        // Verify the orderby param.
        if (tokenType.equals(TokenQueryType.ORDERBY)) {
          try {
            OrderByValueType.fromToken(tokenValue);
          } catch (IllegalArgumentException e) {
            String msg = "Invalid orderby query value: " + tokenValue;
            throw new InvalidQueryException(msg);
          }
        }
        Set<String> valuesPerToken = tokensMap.get(tokenType);
        if (valuesPerToken == null) {
          valuesPerToken = Sets.newLinkedHashSet();
          tokensMap.put(tokenType, valuesPerToken);
        }
        valuesPerToken.add(tokenValue);
      }
      return tokensMap;
    }
    
    /**
     * Builds a list of participants to serve as the filter for the query.
     * 
     * @param queryParams the query params.
     * @param queryType the filter for the query , i.e. 'with'.
     * @return the participants list for the filter.
     * @throws InvalidParticipantAddress if participant id passed to the query is invalid.
     */
    static List<ParticipantId> buildValidatedParticipantIds(
        Map<QueryHelper.TokenQueryType, Set<String>> queryParams,
        QueryHelper.TokenQueryType queryType) throws InvalidParticipantAddress {
      Set<String> tokenSet = queryParams.get(queryType);
      List<ParticipantId> participants = null;
      if (tokenSet != null) {
        participants = Lists.newArrayListWithCapacity(tokenSet.size());
        for (String token : tokenSet) {
          ParticipantId otherUser = ParticipantId.of(token);
          participants.add(otherUser);
        }
      } else {
        participants = Collections.emptyList();
      }
      return participants;
    }
    
    /**
     * Computes ordering for the search results. If none are specified - then
     * returns the default ordering. The resulting ordering is always compounded
     * with ordering by wave id for stability.
     */
    static Ordering<WaveViewData> computeSorter(
        Map<QueryHelper.TokenQueryType, Set<String>> queryParams) {
      Ordering<WaveViewData> ordering = null;
      Set<String> orderBySet = queryParams.get(QueryHelper.TokenQueryType.ORDERBY);
      if (orderBySet != null) {
        for (String orderBy : orderBySet) {
          QueryHelper.OrderByValueType orderingType =
              QueryHelper.OrderByValueType.fromToken(orderBy);
          if (ordering == null) {
            // Primary ordering.
            ordering = orderingType.getOrdering();
          } else {
            // All other ordering are compounded to the primary one.
            ordering = ordering.compound(orderingType.getOrdering());
          }
        }
      } else {
        ordering = QueryHelper.DEFAULT_ORDERING;
      }
      // For stability order also by wave id.
      ordering = ordering.compound(QueryHelper.ID_COMPARATOR);
      return ordering;
    }
  }

  private static final Log LOG = Log.get(WaveMap.class);
  
  private final QueryHelper queryHelper = QueryHelper.newQueryHelper();

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
    LOG.fine("Search query '" + query + "' from user: " + user + " [" + startAt + ", "
        + (startAt + numResults - 1) + "]");
    if (!query.contains("in:inbox")) {
      LOG.warning("Only queries for the inbox work");
      return Collections.emptyList();
    }
    
    Map<QueryHelper.TokenQueryType, Set<String>> queryParams = null;
    try {
      queryParams = queryHelper.parseQuery(query);
    } catch (QueryHelper.InvalidQueryException e1) {
      // Invalid query param - stop and return empty search results.
      LOG.warning("Invalid Query. " + e1.getMessage());
      return Collections.emptyList();
    }
    List<ParticipantId> withParticipantIds = null;
    List<ParticipantId> creatorParticipantIds = null;
    try {
      // Build and validate.
      withParticipantIds =
          QueryHelper.buildValidatedParticipantIds(queryParams, QueryHelper.TokenQueryType.WITH);
      creatorParticipantIds =
        QueryHelper.buildValidatedParticipantIds(queryParams, QueryHelper.TokenQueryType.CREATOR);
    } catch (InvalidParticipantAddress e) {
      // Invalid address - stop and return empty search results.
      LOG.warning("Invalid participantId: " + e.getAddress() + " in query: " + query);
      return Collections.emptyList();
    }
    // Must use a map with stable ordering, since indices are meaningful.
    Map<WaveId, WaveViewData> results = Maps.newLinkedHashMap();
    for (Map.Entry<WaveId, Wave> entry : waves.entrySet()) {
      WaveId waveId = entry.getKey();
      Wave wave = entry.getValue();
      WaveViewData view = null;  // Copy of the wave built up for search hits.
      for (WaveletContainer c : wave) {
        try {
          // Only filtering by participants is implemented for now.
          if (!c.hasParticipant(user) || !matches(c, withParticipantIds, creatorParticipantIds)) {
            continue;
          }
          if (view == null) {
            view = WaveViewDataImpl.create(waveId);
            results.put(waveId, view);
          }
          // Just keep adding all the relevant wavelets in this wave.
          view.addWavelet(c.copyWaveletData());
        } catch (WaveletStateException e) {
          LOG.warning("Failed to access wavelet " + c.getWaveletName(), e);
        }
      }
    }
    Collection<WaveViewData> searchResults = results.values();
    List<WaveViewData> searchResultslist = CollectionUtils.newLinkedList(searchResults);
    int searchResultSize = searchResultslist.size();
    // Check if we have enough results to return.
    if (searchResultSize < startAt) {
      searchResultslist = Collections.emptyList();
    } else {
      int endAt = Math.min(startAt + numResults, searchResultSize);
      searchResultslist =
          QueryHelper.computeSorter(queryParams).sortedCopy(searchResultslist)
              .subList(startAt, endAt);
    }
    LOG.info("Search response to '" + query + "': " + searchResultslist.size() + " results");
    return searchResultslist;
  }

  /** Verifies whether the wavelet matches the filter criteria */
  private static boolean matches(WaveletContainer c, List<ParticipantId> withList,
      List<ParticipantId> creatorList) throws WaveletStateException {
    // First filter by creator
    for (ParticipantId creator : creatorList) {
      if (!creator.equals(c.getCreator())) {
        // Skip.
        return false;
      }
    }
    // Now filter by 'with'.
    for (ParticipantId otherUser : withList) {
      // Each call takes and releases the WaveletContainer lock.
      if (!c.hasParticipant(otherUser)) {
        // Skip.
        return false;
      }
    }
    return true;
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
