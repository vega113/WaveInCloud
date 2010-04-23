// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

/**
 * A read only interface to a set of elements.
 *
 * This is used in favor of using a standard Java collections interface so that
 * Javascript-optimized implementations can be used in GWT code.
 *
 * Null is not permitted as a value. All methods, including
 * {@link #contains(Object)}, will reject null values.
 *
 * @author ohler@google.com (Christian Ohler)
*
 */
public interface ReadableIdentitySet<T> {

  /**
   * A procedure that accepts an element from the set.
   */
  public interface Proc<T> {
    void apply(T element);
  }

  /** @return true if and only if {@code s} is in this set. */
  boolean contains(T s);

  /** @return true if and only if this set is empty. */
  boolean isEmpty();

  /**
   * Calls a procedure with each element in this set, in undefined order.
   */
  void each(Proc<? super T> procedure);

  /**
   * @return the size of this set. Note: Depending on the underlying
   *         implementation, this may be a linear operation.
   */
  int countEntries();
}
