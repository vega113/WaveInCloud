// Copyright 2010 Google Inc. All Rights Reserved

package org.waveprotocol.wave.model.util;

import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * An observable map.
 *
*
 */
public interface ObservableMap<K, V> extends ReadableMap<K, V>,
    SourcesEvents<ObservableMap.Listener<? super K, ? super V>> {

  /**
   * Observer of map changes.
   */
  interface Listener<K, V> {
    /**
     * Notifies this listener that a map entry has been added.
     */
    void onEntryAdded(K key, V value);

    /**
     * Notifies this listener that a map entry has been removed.
     */
    void onEntryRemoved(K key, V value);
  }
}
