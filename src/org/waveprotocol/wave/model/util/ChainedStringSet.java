// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

/**
 * Chained StringSet implementation
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ChainedStringSet extends ChainedData<ReadableStringSet, StringSet> {

  /**
   * Root
   */
  public ChainedStringSet() {
    super(CollectionUtils.STRING_SET_DOMAIN);
  }

  /**
   * Chained
   */
  public ChainedStringSet(ChainedData<ReadableStringSet, StringSet> parent) {
    super(parent);
  }

  /** Create a child set */
  public ChainedStringSet createExtension() {
    return new ChainedStringSet(this);
  }
}