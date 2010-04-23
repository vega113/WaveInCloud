// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;


import java.util.Queue;

/**
 * A factory interface for creating the types of collections that we
 * have optimized JavaScript implementations for.
 */
public interface CollectionFactory {
  /**
   * Returns a new, empty StringMap.
   */
  <V> StringMap<V> createStringMap();

  /**
   * Returns a new, empty NumberMap.
   */
  <V> NumberMap<V> createNumberMap();

  /**
   * Returns a new, empty IntMap.
   */
  <V> IntMap<V> createIntMap();

  /**
   * Returns a new, empty StringSet.
   */
  <V> StringSet createStringSet();

  /**
   * Returns a new, empty IdentitySet.
   */
  <T> IdentitySet<T> createIdentitySet();

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
  <K, V> IdentityMap<K, V> createIdentityMap();
}
