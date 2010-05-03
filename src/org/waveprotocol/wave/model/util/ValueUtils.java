// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

/**
 * Utilities related to the document-based value classes
 *
 * @author anorth@google.com (Alex North)
 */
public final class ValueUtils {

  /**
   * @return true iff a and b are both null or are equal
   */
  public static <T> boolean equal(T a, T b) {
    return (a == null) ? (b == null) : a.equals(b);
  }

  /**
   * @return true iff a and b are not both null and are not equal
   */
  public static <T> boolean notEqual(T a, T b) {
    return (a == null) ? (b != null) : !a.equals(b);
  }

  /**
   * @return {@code value} if it is not null, {@code def} otherwise
   */
  public static <T> T valueOrDefault(T value, T def) {
    return value != null ? value : def;
  }
}
