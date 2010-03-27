// Copyright 2008 Google Inc. All Rights Reserved

package org.waveprotocol.wave.model.util;

/**
 * A subset of the {@link java.util.Map} interface.  This interface is used so
 * that Map implementations may be simpler (e.g., no iteration), but still
 * present a familiar API.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <K>
 * @param <V>
 */
public interface SimpleMap<K, V> extends ReadableMap<K, V> {

  /** @see java.util.Map#put(Object,Object) */
  V put(K key, V value);

  /** @see java.util.Map#remove(Object) */
  V remove(K key);

  /** @see java.util.Map#clear() */
  void clear();
}
