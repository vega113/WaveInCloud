// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.document.util.DocumentContext;

/**
 * Tentative annotation change listener
 *
 * The changes map more or less to the operations received, with minimal optimisation.
 * Importantly, a large single change that is equivalent to many small changes (for
 * example, if we bold a large region that has every second character already bolded)
 * will still come up as a single large event.
 *
 * TODO(danilatos): Consider using Point rather than int as the location type
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface AnnotationMutationHandler {

  /**
   * Notification that a change occurred.
   *
   * @param start
   * @param end
   * @param key
   * @param newValue The new value, possibly null
   */
  <N, E extends N, T extends N> void handleAnnotationChange(
      DocumentContext<N, E, T> bundle,
      int start, int end, String key, Object newValue);
}
