/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.waveprotocol.wave.client.scroll;

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.MeasurerInstance;

/**
 * A target-based scroller that tries to be smart by making aesthetically
 * pleasing viewport movements.
 *
 * @param <M> type of target entities to which this scroller can move
 */
public final class SmartScroller<M> implements TargetScroller<M> {

  /**
   * Defines how to move the viewport between two extents.
   */
  enum ScrollStrategy {
    SMART {
      @Override
      double move(Extent from, Extent target, Extent viewport) {
        // Constraints:
        // 0. The target top must end up in the viewport.
        // 1. The target region must be maximally within the viewport (no
        // viewport shift could include more of the target).
        // 2. If the target is already enclosed by the viewport, there is no
        // movement.
        // 3. If it is a valid location by the other constraints, the target
        // must appear at the viewport location of the previously focused
        // target.
        // Otherwise, the viewport should be moved minimally to bring the target
        // to a valid location.

        // Constraint 0 determines an initial range.
        double minStart = target.getEnd() - viewport.getSize();
        double maxStart = target.getStart();

        // Target too big?
        if (minStart >= maxStart) {
          // Constraint 1 determines the answer.
          return maxStart;
        }

        // Already valid?
        if (minStart <= viewport.getStart() && viewport.getStart() <= maxStart) {
          // Contraint 2 determines the answer.
          return viewport.getStart();
        }

        // Is previous location good?
        if (from != null) {
          double stableStart = viewport.getStart() + target.getStart() - from.getStart();
          if (minStart <= stableStart && stableStart <= maxStart) {
            // Constraint 3 determines the answer.
            return stableStart;
          }
        }

        // Pick minimal movement. We know current viewport start is either
        // before the min or after the max.
        if (viewport.getStart() < minStart) {
          return minStart;
        } else {
          assert viewport.getStart() > maxStart;
          return maxStart;
        }
      }
    },
    // Other strategies may turn up over time.
    ;

    /**
     * @param from location of the previously focused target, or {@code null}
     * @param target location of the new target (never {@code null})
     * @param viewport location of the viewport (never {@code null})
     * @return the new viewport location for a shift from previously focused
     *         {@code from} to new target {@code to}, with current viewport
     *         location of {@code viewport}.
     */
    abstract double move(Extent from, Extent target, Extent viewport);
  }

  private final PositionalScroller panel;
  private final Measurer<? super M> measurer;

  /** Last element brought in to view. Used for smart scrolling. */
  private M previousTarget;

  @VisibleForTesting
  SmartScroller(PositionalScroller panel, Measurer<? super M> measurer) {
    this.panel = panel;
    this.measurer = measurer;
  }

  /**
   * Creates a scroller for a DOM element. It is assumed that the given element
   * has appropriate CSS constraints that make it scrollable.
   */
  public static SmartScroller<Element> create(Element e) {
    ScrollPanel panel = new ScrollPanel(e, MeasurerInstance.get());
    return new SmartScroller<Element>(panel, panel);
  }

  @Override
  public void moveTo(M target) {
    ScrollStrategy style = ScrollStrategy.SMART;

    Extent from = previousTarget != null ? measurer.extentOf(previousTarget) : null;
    Extent to = measurer.extentOf(target);
    Extent viewport = panel.getViewport();
    panel.moveTo(style.move(from, to, viewport));

    previousTarget = target;
  }
}
