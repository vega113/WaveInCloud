// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

/**
 * The representation of a ranged annotation, which is a key-value pair and the
 * range annotated with it.  The range is maximal in that the values associated
 * with the key before the start and after the end of the range differ from the
 * value inside the range.
 *
 * @author ohler@google.com (Christian Ohler)
 *
 * @param <V> the type of values of annotations
 */
public interface RangedAnnotation<V> {
  /** The key of the key-value pair. */
  String key();

  /** The value of key-value pair. */
  V value();

  /** The index of the first item covered by this annotation. */
  int start();

  /** The index beyond the last item covered by this annotation. */
  int end();
}
