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

import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.List;

/**
 * An ordered collection of asynchronously-loaded digests.
 * <p>
 * A search result is a fixed-size list of digests, any of which may be null.
 * Each transition from {@link Search.State#SEARCHING} to {@link Search.State#READY} indicates
 * a fresh, new search result. A search result may evolve in three ways:
 * <ul>
 * <li>a digest entry become populated (
 * {@link Search.Listener#onDigestsReady});</li>
 * <li>a new digest entry is inserted into the result, increasing its size (
 * {@link Search.Listener#onDigestAdded}); and</li>
 * <li>a digest entry is removed from the result, decreasing its size (
 * {@link Search.Listener#onDigestRemoved}).</li>
 * </ul>
 *
 * @author hearnden@google.com (David Hearnden)
 */
public interface Search extends SourcesEvents<Search.Listener> {

  /**
   * Observes changes to the search result.
   */
  public interface Listener {
    /**
     * Notifies this listener that the search's {@link #getState state} has
     * changed.
     */
    void onStateChanged();

    //
    // The following events are not currently provided by any search
    // implementation.
    //

    /**
     * Notifies this listener of positions in the search where data is now
     * available.
     */
    void onDigestsReady(List<Integer> indices);

    /**
     * Notifies this listener that a new entry has been inserted into the results.
     */
    void onDigestAdded(int index, Digest digest);

    /**
     * Notifies this listener that a digest has been removed from the results.
     */
    void onDigestRemoved(int index, Digest digest);
  }

  enum State {
    /**
     * Search is ready for queries.
     */
    READY,
    /**
     * A search is underway.
     */
    SEARCHING,
  }

  /**
   * @return the current search state.
   */
  State getState();

  /**
   * Performs a search. This search's state will become {@link State#SEARCHING},
   * and then {@link State#READY} once some results are ready.
   *
   * @param query search query
   */
  void find(String query);

  /**
   * Stops the current search if there is one.
   */
  void cancel();

  /**
   * @return the total number of results in this search.
   */
  int getTotal();

  /**
   * @return the digest at position {@code index} in the search result. This may
   *         return null, indicating that the digest at that location has not
   *         yet been loaded.
   */
  Digest getDigest(int index);
}
