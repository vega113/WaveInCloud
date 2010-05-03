// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.indexed;

/**
 * Simple listener for changes to annotations.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface AnnotationSetListener<V> {

  /**
   * Called when a potential annotation change occurs.
   *
   * The caller need not guarantee that all of the range did not already have
   * the new value for the given key.
   *
   * @param start beginning of the range
   * @param end end of the range
   * @param key key that changed
   * @param newValue new value, which may be null
   */
  void onAnnotationChange(int start, int end, String key, V newValue);
}
