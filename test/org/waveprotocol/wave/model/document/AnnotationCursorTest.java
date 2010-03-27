// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.indexed.RawAnnotationSet;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringSet;

import java.util.NoSuchElementException;

/**
 * Abstract test for implementations of {@link AnnotationCursor}
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public abstract class AnnotationCursorTest extends TestCase {

  protected abstract RawAnnotationSet<Object> getNewSet();

  public void testNoChange() throws OperationException {
    RawAnnotationSet<Object> m = getNewSet();

    m.begin(false);
    m.insert(100);
    m.finish();

    expectFinished(m.annotationCursor(40, 60, strs()));
    expectFinished(m.annotationCursor(40, 60, strs("a", "b")));
    expectFinished(m.annotationCursor(0, 100, strs()));
    expectFinished(m.annotationCursor(0, 100, strs("a", "b")));

    m.begin(false);
    m.skip(10);
    m.startAnnotation("a", "1");
    m.skip(80);
    m.endAnnotation("a");
    m.finish();

    expectFinished(m.annotationCursor(40, 60, strs()));
    expectFinished(m.annotationCursor(40, 60, strs("a")));
    expectFinished(m.annotationCursor(40, 60, strs("a", "b")));

    m.begin(false);
    m.skip(20);
    m.startAnnotation("b", "2");
    m.skip(60);
    m.endAnnotation("b");
    m.finish();

    expectFinished(m.annotationCursor(40, 60, strs()));
    expectFinished(m.annotationCursor(40, 60, strs("a")));
    expectFinished(m.annotationCursor(40, 60, strs("a", "b")));
    expectFinished(m.annotationCursor(40, 60, strs("a", "b", "c")));

    m.begin(false);
    m.skip(25);
    m.startAnnotation("a", "3");
    m.skip(70);
    m.endAnnotation("a");
    m.finish();

    expectFinished(m.annotationCursor(40, 60, strs()));
    expectFinished(m.annotationCursor(40, 60, strs("a")));
    expectFinished(m.annotationCursor(40, 60, strs("a", "b")));
    expectFinished(m.annotationCursor(40, 60, strs("a", "b", "c")));
  }

  public void testChange() throws OperationException {
    RawAnnotationSet<Object> m = fancySet();

    for (int i = 0; i <= 1; i++) {
      AnnotationCursor c1 = m.annotationCursor(i, 100 - i, strs("a"));
      AnnotationCursor c2 = m.annotationCursor(i, 100 - i, strs("a", "b"));
      AnnotationCursor c3 = m.annotationCursor(i, 100 - i, strs("a", "b", "c"));
      AnnotationCursor c4 = m.annotationCursor(i, 100 - i, strs("c", "d"));
      AnnotationCursor ca = m.annotationCursor(i, 100 - i, strs("a", "b", "c", "d"));
      AnnotationCursor cx = m.annotationCursor(i, 100 - i, strs("x", "y", "z"));

      expectFinished(cx);

      checkAdvance(c1, 10, "a");
      checkAdvance(c1, 30, "a");
      checkAdvance(c1, 60, "a");
      expectFinished(c1);

      checkAdvance(c2, 10, "a");
      checkAdvance(c2, 20, "b");
      checkAdvance(c2, 30, "a");
      checkAdvance(c2, 40, "b");
      checkAdvance(c2, 50, "b");
      checkAdvance(c2, 60, "a");
      expectFinished(c2);

      checkAdvance(c3, 10, "a");
      checkAdvance(c3, 20, "b");
      checkAdvance(c3, 30, "a", "c");
      checkAdvance(c3, 40, "b", "c");
      checkAdvance(c3, 50, "b");
      checkAdvance(c3, 60, "a");
      expectFinished(c3);

      checkAdvance(c4, 30, "c", "d");
      checkAdvance(c4, 40, "c", "d");
      expectFinished(c4);

      checkAdvance(ca, 10, "a");
      checkAdvance(ca, 20, "b");
      checkAdvance(ca, 30, "a", "c", "d");
      checkAdvance(ca, 40, "b", "c", "d");
      checkAdvance(ca, 50, "b");
      checkAdvance(ca, 60, "a");
      expectFinished(ca);
    }

    AnnotationCursor c5 = m.annotationCursor(30, 40, strs("c", "d"));
    expectFinished(c5);

    c5 = m.annotationCursor(29, 40, strs("c", "d"));
    checkAdvance(c5, 30, "c", "d");
    expectFinished(c5);

    c5 = m.annotationCursor(30, 41, strs("c", "d"));
    checkAdvance(c5, 40, "c", "d");
    expectFinished(c5);
  }

  public void testInitialStartLocation() throws OperationException {

    RawAnnotationSet<Object> m = getNewSet();

    m.begin(false);
    m.insert(100);
    m.finish();

    // -1 if no hasNext()
    AnnotationCursor c = m.annotationCursor(40, 80, strs("a"));
    assertEquals(-1, c.currentLocation());

    m.begin(false);
    m.skip(50);
    m.startAnnotation("a", "1");
    m.skip(10);
    m.endAnnotation("a");
    m.finish();

    // start value, if hasNext();
    c = m.annotationCursor(40, 80, strs("a"));
    assertEquals(40, c.currentLocation());

  }

  protected RawAnnotationSet<Object> fancySet() throws OperationException {
    RawAnnotationSet<Object> m = getNewSet();

    m.begin(false);
    m.insert(100);
    m.finish();

    m.begin(false);
    m.skip(10);
    m.startAnnotation("a", "1");
    m.skip(10);
    m.startAnnotation("b", "2");
    m.skip(10);
    m.startAnnotation("c", "3");
    m.startAnnotation("a", "3");
    m.startAnnotation("d", "3");
    m.skip(10);
    m.endAnnotation("c");
    m.endAnnotation("d");
    m.startAnnotation("b", "4");
    m.skip(10);
    m.endAnnotation("b");
    m.skip(10);
    m.endAnnotation("a");
    m.finish();

    return m;
  }

  protected void checkAdvance(AnnotationCursor cursor, int location, String ... keys) {
    assertTrue(cursor.hasNext());
    assertEquals(
        CollectionUtils.newJavaSet(strs(keys)),
        CollectionUtils.newJavaSet(cursor.nextLocation()));
    assertEquals(location, cursor.currentLocation());
  }

  protected void expectFinished(AnnotationCursor cursor) {
    assertFalse(cursor.hasNext());
    try {
      cursor.nextLocation();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException e) {
      // OK
    }
  }

  protected StringSet strs(String ... strings) {
    return CollectionUtils.newStringSet(strings);
  }

  public void testEmptySet() {
    RawAnnotationSet<Object> m = getNewSet();

    m.begin(false);
    m.insert(1);
    m.startAnnotation("a", "1");
    m.insert(1);
    m.endAnnotation("a");
    m.finish();

    expectFinished(m.annotationCursor(0, m.size(), strs()));
  }
}
