// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.util.ReadableStringSet;

/**
 * A cursor for a set of keys over a range in an annotation set. Advancing the
 * cursor moves forward to the nearest change in value of any key, until the
 * range has been traversed.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface AnnotationCursor {

  /**
   * Proceeds to the next location and returns the keys whose values are
   * changing at that location.
   *
   * Initially, the current location is the start of the cursor's range. After
   * the first call to this method, the location will be at the first change of
   * value in the relevant key set away from the initial values at the start
   * location.
   *
   * @return the keys whose values are changing at that location
   */
  ReadableStringSet nextLocation();

  /**
   * Gets the current location of the cursor.
   *
   * @return The current location (item index into the document) of the cursor
   */
  int currentLocation();

  /**
   * @return true if there are still changes in value remaining in the range
   *         from the current location
   */
  boolean hasNext();
}
