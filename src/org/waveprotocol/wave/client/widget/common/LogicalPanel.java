/**
 * Copyright 2010 Google Inc.
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
package org.waveprotocol.wave.client.widget.common;

import com.google.gwt.user.client.ui.Widget;

/**
 * A panel that can adopt and orphan widgets that attach and detach themselves
 * from existing elements.
 *
 */
public interface LogicalPanel {
  //
  // These names are intentionally different from Panel's adopt() and orphan()
  // methods, because those methods are protected.
  //

  /**
   * Logically attaches a widget to this panel, without any physical DOM change.
   * It is assumed that {@code child} is already a physical descendant of this
   * panel.
   *
   * @param child widget to adopt
   */
  void doAdopt(Widget child);

  /**
   * Logically detaches a child from this panel, without any physical DOM
   * change. It is assumed that {@code child} is currently a physical descendant
   * of this panel.
   *
   * @param child widget to orphan
   */
  void doOrphan(Widget child);
}
