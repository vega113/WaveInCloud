// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.RangedAnnotationIterableTest;
import org.waveprotocol.wave.model.document.indexed.AnnotationTree;
import org.waveprotocol.wave.model.util.ReadableStringSet;

/**
 * Test for GenericRangedAnnotationIterable.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class GenericRangedAnnotationIterableTest
    extends RangedAnnotationIterableTest<AnnotationTree<Object>> {

  @Override
  protected Iterable<RangedAnnotation<Object>> getIterable(AnnotationTree<Object> set, int start,
      int end, ReadableStringSet keys) {
    return new GenericRangedAnnotationIterable<Object>(set, start, end, keys);
  }

  @Override
  protected AnnotationTree<Object> getNewSet() {
    return new AnnotationTree<Object>("a", "b", null);
  }

}
