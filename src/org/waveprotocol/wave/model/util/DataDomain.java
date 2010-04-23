// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

/**
 * A typeclass-style interface for manipulating mutable data
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <T> The data type (e.g. StringSet)
 * @param <R> A read-only version of the data type (e.g. ReadableStringSet).
 *   If none is available, then this may be the same as <T>.
 */
public interface DataDomain<R, T extends R> {

  /**
   * @return a new empty instance of the data
   */
  T empty();

  /**
   * Updates {@code target} to be the result of combining {@code changes} onto
   * {@code base}
   *
   * @param target
   * @param changes
   * @param base
   */
  void compose(T target, R changes, R base);

  /**
   * @param modifiable
   * @return optionally a read-only view of the modifiable data, to provide
   *         runtime readonly guarantees. Otherwise just return the parameter.
   */
  R readOnlyView(T modifiable);
}
