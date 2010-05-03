// Copyright 2008 Google Inc. All Rights Reserved

package org.waveprotocol.wave.model.util;

/**
 * A simplification of the {@link java.util.Map} interface. This interface is
 * used so that Map implementations may be simpler (e.g., no values or entries
 * collections), but still present a familiar API.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <K>
 * @param <V>
 */
public interface ReadableMap<K, V> {

  /** @see java.util.Map#isEmpty() */
  boolean isEmpty();

  /** @see java.util.Map#get(Object) */
  V get(K key);

  /** @see java.util.Map#size() */
  int size();

  /**
   * @return a copy of the current keys. The returned object is a snapshot of
   *         the current key collection, unlike {@link java.util.Map#keySet()},
   *         is not updated when this map changes, nor do changes to this copy
   *         propagate into the map. It is safe to modify the map while
   *         iterating through the returned snapshot object.
   */
  Iterable<K> copyKeys();
}
