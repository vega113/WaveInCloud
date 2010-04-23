// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

/**
 * A read-write interface to a set of elements.
 *
 * This is used in favor of using a standard Java collections interface so that
 * Javascript-optimized implementations can be used in GWT code.
 *
 * Consistent with {@link ReadableIdentitySet}, null is not permitted as a
 * value. All methods will reject null values.
 *
*
 */
public interface IdentitySet<T> extends ReadableIdentitySet<T> {

  /**
   * Adds that an element to this set it is it not already present. Otherwise,
   * does nothing.
   *
   * @param s element to add
   */
  void add(T s);

  /**
   * Removes an element from this set if it is present. Otherwise, does nothing.
   *
   * @param s element to remove
   */
  void remove(T s);

  /**
   * Removes all elements from this set.
   */
  void clear();
}
