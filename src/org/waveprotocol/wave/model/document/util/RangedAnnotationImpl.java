// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.RangedAnnotation;

import org.waveprotocol.wave.model.util.Preconditions;

/**
 * An implementation of RangedAnnotation that simply holds values passed to it.
 *
 * This class has a mutator so that instances can be reused.
 *
 * @author ohler@google.com (Christian Ohler)
 *
 * @param <V> the type of annotation values
 */
public final class RangedAnnotationImpl<V> implements RangedAnnotation<V> {
  private String key;
  private V value;
  private int start;
  private int end;

  public RangedAnnotationImpl(String key, V value, int start, int end) {
    set(key, value, start, end);
  }

  public RangedAnnotationImpl(RangedAnnotation<V> other) {
    this(other.key(), other.value(), other.start(), other.end());
  }

  public void set(String key, V value, int start, int end) {
    // We don't have access to the size of the annotation set, but we can
    // still check 0 <= start <= end.
    Preconditions.checkPositionIndexes(start, end, Integer.MAX_VALUE);
    Preconditions.checkNotNull(key, "key must not be null");
    if (start >= end) {
      throw new IllegalArgumentException("Attempt to set RangedAnnotation to zero length");
    }
    this.key = key;
    this.value = value;
    this.start = start;
    this.end = end;
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public V value() {
    return value;
  }

  @Override
  public int start() {
    return start;
  }

  @Override
  public int end() {
    return end;
  }

  @Override
  public String toString() {
    return "RangedAnnotationImpl(" + key + ", " + value + ", " + start + ", " + end + ")";
  }

}
