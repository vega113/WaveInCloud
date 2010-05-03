// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.indexed.AnnotationTree;
import org.waveprotocol.wave.model.util.ReadableStringSet;

/**
 * @author ohler@google.com (Christian Ohler)
 */
public class GenericAnnotationIntervalIterableTest
    extends AnnotationIntervalIterableTest<AnnotationTree<Object>> {

  @Override
  protected Iterable<AnnotationInterval<Object>> getIterable(AnnotationTree<Object> set, int start,
      int end, ReadableStringSet keys) {
    return new GenericAnnotationIntervalIterable<Object>(set, start, end, keys);
  }

  @Override
  protected AnnotationTree<Object> getNewSet() {
    return new AnnotationTree<Object>("a", "b", null);
  }

}
