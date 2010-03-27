// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

/**
 * Simple predicate function interface
 *
 * (Do we already have one as of 18/Aug/09?)
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface Predicate<T> {

  /**
   * @param input
   * @return the result of this predicate on the input
   */
  boolean apply(T input);
}
