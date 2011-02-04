/**
 * Copyright 2011 Google Inc.
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
package org.waveprotocol.box.webclient.search;

import org.waveprotocol.box.webclient.search.SearchService.Callback;
import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;

import java.util.List;

/**
 * A simple implementation of the search model, using a search service.
 * <p>
 * This search keeps a list that corresponds to the total search result size.
 * Segments of that list are filled in as necessary.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class SimpleSearch implements Search {

  private final static LoggerBundle log = new DomLogger("search");

  /** Service that performs searches. */
  private final SearchService searcher;

  /**
   * A list the size of the total search result, populated with digests that are
   * known to this search model.
   */
  private final List<Digest> digests = CollectionUtils.newArrayList();

  /** Listeners. */
  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  /** The request that is currently in flight, or {@code null}. */
  private Callback outstanding;

  /**
   * Creates a search model.
   *
   * @param searcher service that performs searches
   */
  public SimpleSearch(SearchService searcher) {
    this.searcher = searcher;
  }

  @Override
  public void find(String query) {
    Callback request = new Callback() {
      @Override
      public void onFailure(String message) {
        if (outstanding == this) {
          outstanding = null;
          handleFailure(message);
        }
      }

      @Override
      public void onSuccess(int total, List<? extends Digest> snapshots) {
        if (outstanding == this) {
          outstanding = null;
          handleSuccess(total, 0, snapshots);
        }
      }
    };

    // Clear current search.
    digests.clear();

    if (outstanding == null) {
      outstanding = request;
      // TODO (Yuri/David) Need to handle the index and numResults parameters.
      // Index probably should always stay 0, while the numResults can grow - in
      // case the user scrolls down the wave list panel and more search results
      // should be requested from the server and rendered for user.
      searcher.search(query, 0, 50, request);
      fireOnStateChanged();
    } else {
      outstanding = request;
      searcher.search(query, 0, 50, request);
    }
  }

  @Override
  public void cancel() {
    handleFailure("cancelled by user");
  }

  /**
   * Logs an error.  Does not affect any current results.
   */
  private void handleFailure(String message) {
    log.error().log("Search failed: ", message);
    fireOnStateChanged();
  }

  /**
   * Copies the digest snapshots into this search result's state.
   */
  private void handleSuccess(int total, int from, List<? extends Digest> digests) {
    int min = Math.min(this.digests.size(), digests.size());
    for (int i = 0; i < min; i++) {
      this.digests.set(i, digests.get(i));
    }
    while (this.digests.size() < digests.size()) {
      this.digests.add(digests.get(this.digests.size()));
    }
    while (digests.size() < this.digests.size()) {
      this.digests.remove(this.digests.size() - 1);
    }

    fireOnStateChanged();
  }

  @Override
  public State getState() {
    return outstanding == null ? State.READY : State.SEARCHING;
  }

  @Override
  public Digest getDigest(int index) {
    return digests.get(index);
  }

  @Override
  public int getTotal() {
    return digests.size();
  }

  //
  // Events.
  //

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void fireOnStateChanged() {
    for (Listener listener : listeners) {
      listener.onStateChanged();
    }
  }
}
