// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;


import java.util.Queue;

/**
 * A factory interface for creating the types of collections that we
 * have optimized JavaScript implementations for.
 *
 * @param <V> the type of values in StringMaps
 */
public interface CollectionFactory<V> {
  /**
   * Returns a new, empty StringMap.
   */
  StringMap<V> createStringMap();

  /**
   * Returns a new, empty NumberMap.
   */
  NumberMap<V> createNumberMap();

  /**
   * Returns a new, empty IntMap.
   */
  IntMap<V> createIntMap();

  /**
   * Returns a new, empty StringSet.
   */
  StringSet createStringSet();

  /**
   * Returns a queue.
   */
  <E> Queue<E> createQueue();

  /**
   * Returns a priority queue.
   */
  NumberPriorityQueue createPriorityQueue();

  /**
   * Returns an identity map.
   */
  <K> IdentityMap<K, V> createIdentityMap();
}
