// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

import java.util.Set;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ChainedHashSet<T> extends ChainedData<Set<T>, Set<T>> {

  public static final ChainedHashSet<Object> EMPTY_ROOT = new ChainedHashSet<Object>();

  @SuppressWarnings("unchecked")
  public static <T> ChainedHashSet<T> emptyRoot() {
    return (ChainedHashSet<T>) EMPTY_ROOT;
  }

  /**
   * Root
   */
  public ChainedHashSet() {
    super(CollectionUtils.<T>hashSetDomain());
  }

  /**
   * Chained
   */
  public ChainedHashSet(ChainedHashSet<T> parent) {
    super(parent);
  }

  /** Create a child set */
  public ChainedHashSet<T> createExtension() {
    return new ChainedHashSet<T>(this);
  }
}
