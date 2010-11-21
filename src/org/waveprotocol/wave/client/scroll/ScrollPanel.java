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

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.Measurer;
import org.waveprotocol.wave.client.scroll.TargetScroller.Extent;

/**
 * Reveals the scrolling capabilities of a DOM element.
 *
 */
public final class ScrollPanel implements TargetScroller.Measurer<Element>, PositionalScroller {
  private final Element element;
  private final Measurer measurer;

  ScrollPanel(Element element, Measurer measurer) {
    this.element = element;
    this.measurer = measurer;
  }

  @Override
  public Extent getViewport() {
    int top = element.getScrollTop();
    return Extent.of(top, top + measurer.height(element));
  }

  @Override
  public Extent extentOf(Element target) {
    int top = element.getScrollTop();
    return Extent.of(measurer.top(element, target) + top, measurer.bottom(element, target) + top);
  }

  @Override
  public void moveTo(double location) {
    element.setScrollTop((int) location);
  }
}
