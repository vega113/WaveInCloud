// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.AnnotationCursor;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.indexed.RawAnnotationSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import java.util.List;

/**
 * Presents a mutable view over a raw annotation set that permits object values,
 * does not emit operations, and enforces that the keys use the local annotation
 * prefix to avoid mutating the persistent view of annotations.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class LocalAnnotationSetImpl implements MutableAnnotationSet.Local {

  /***/
  protected final RawAnnotationSet<Object> fullAnnotationSet;

  /**
   * @param fullAnnotationSet substrate
   */
  public LocalAnnotationSetImpl(RawAnnotationSet<Object> fullAnnotationSet) {
    this.fullAnnotationSet = fullAnnotationSet;
  }

  /**
   * Checks that the given key is a valid local key - does nothing if it is
   * @param key key to check
   * @throws IllegalArgumentException if the key is not valid
   */
  protected final void checkLocalKey(String key) {
    if (!Annotations.isLocal(key)) {
      throw new IllegalArgumentException("Not a local annotation key: " + key);
    }
  }

  @Override
  public void setAnnotation(int start, int end, String key, Object value) {
    Preconditions.checkPositionIndexes(start, end, fullAnnotationSet.size());
    checkLocalKey(key);
    if (end - start > 0) {
      fullAnnotationSet.begin(false);
      if (start > 0) {
        fullAnnotationSet.skip(start);
      }
      fullAnnotationSet.startAnnotation(key, value);
      if (end - start > 0) {
        fullAnnotationSet.skip(end - start);
      }
      fullAnnotationSet.endAnnotation(key);
      fullAnnotationSet.finish();
    }
  }

  @Override
  public void resetAnnotation(int start, int end, String key, Object value) {
    Preconditions.checkPositionIndexes(start, end, fullAnnotationSet.size());
    checkLocalKey(key);
    if (end - start > 0) {
      fullAnnotationSet.begin(false);
      fullAnnotationSet.startAnnotation(key, null);
      if (start > 0) {
        fullAnnotationSet.skip(start);
      }
      fullAnnotationSet.startAnnotation(key, value);
      fullAnnotationSet.skip(end - start);
      fullAnnotationSet.startAnnotation(key, null);
      if (size() - end > 0) {
        fullAnnotationSet.skip(size() - end);
      }
      fullAnnotationSet.endAnnotation(key);
      fullAnnotationSet.finish();
    }
  }
  @Override
  @Deprecated
  public void resetAnnotationsInRange(int rangeStart, int rangeEnd, String key,
      List<RangedValue<Object>> values) {
    throw new RuntimeException("This method is a server side hack only");
  }

  @Override
  public Object getAnnotation(int start, String key) {
    return fullAnnotationSet.getAnnotation(start, key);
  }

  @Override
  public int firstAnnotationChange(int start, int end, String key, Object fromValue) {
    return fullAnnotationSet.firstAnnotationChange(start, end, key, fromValue);
  }

  @Override
  public int lastAnnotationChange(int start, int end, String key, Object fromValue) {
    return fullAnnotationSet.lastAnnotationChange(start, end, key, fromValue);
  }

  @Override
  public int size() {
    return fullAnnotationSet.size();
  }

  @Override
  public AnnotationCursor annotationCursor(int start, int end, ReadableStringSet keys) {
    return new GenericAnnotationCursor<Object>(this, start, end, keys);
  }

  @Override
  public Iterable<AnnotationInterval<Object>> annotationIntervals(int start, int end,
      ReadableStringSet keys) {
    return fullAnnotationSet.annotationIntervals(start, end, keys);
  }

  @Override
  public Iterable<RangedAnnotation<Object>> rangedAnnotations(int start, int end,
      ReadableStringSet keys) {
    return fullAnnotationSet.rangedAnnotations(start, end, keys);
  }

  @Override
  public void forEachAnnotationAt(int location,
      ReadableStringMap.ProcV<Object> callback) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public ReadableStringSet knownKeys() {
    return fullAnnotationSet.knownKeys();
  }
}
