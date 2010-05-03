//Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.document.AnnotationCursorTest;
import org.waveprotocol.wave.model.document.util.GenericAnnotationCursor;

/**
 * Tests the SimpleAnnotationSet data structure
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class GenericAnnotationCursorTest extends AnnotationCursorTest {

  @Override
  protected RawAnnotationSet<Object> getNewSet() {
    SimpleAnnotationSet set = new SimpleAnnotationSet(null);
    // Just to make sure we are in fact testing the generic annotation cursor
    assertTrue(set.annotationCursor(0, 0, strs()) instanceof GenericAnnotationCursor);
    return set;
  }

}
