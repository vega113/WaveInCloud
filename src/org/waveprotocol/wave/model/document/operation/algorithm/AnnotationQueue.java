/**
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.wave.model.document.operation.algorithm;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;

import java.util.ArrayList;
import java.util.List;

/**
 * A queue for annotation boundaries.
 *
 *
 */
abstract class AnnotationQueue {

  private final List<AnnotationBoundaryMap> events = new ArrayList<AnnotationBoundaryMap>();

  /**
   * Do not use this directly.  This is only here for subclasses to override.
   */
  abstract void unqueue(AnnotationBoundaryMap map);

  /**
   * Register a change in annotations at the current location in the mutations
   * being processed as part of a transformation.
   *
   * @param map The annotation transition information.
   */
  final void queue(AnnotationBoundaryMap map) {
    events.add(map);
  }

  /**
   * Flushes any queued annotation events.
   */
  final void flush() {
    for (AnnotationBoundaryMap map : events) {
      unqueue(map);
    }
    events.clear();
  }

}
