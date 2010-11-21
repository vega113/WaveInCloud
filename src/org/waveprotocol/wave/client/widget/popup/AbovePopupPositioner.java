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

package org.waveprotocol.wave.client.widget.popup;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Popup positioner that places the popup above the relative element, aligned to
 * its left side if there is space on the right, or it's right side if there
 * isn't.
 *
 */
public class AbovePopupPositioner implements RelativePopupPositioner {

  // TODO(user): Expose these constants on the resource bundle of the chrome images.

  /**
   * Distance from the bottom edge of the north chrome frame to the top of the
   * blue border line in it.
   */
  private final static int SOUTH_CHROME_OFFSET_PX = 4;

  /**
   * Distance from the left edge of the east chrome frame to the right of the
   * blue border line in it.
   * Non-private only for tests.
   */
  final static int EAST_CHROME_OFFSET_PX = 1;

  /**
   * Distance from the right edge of the west chrome frame to the left of the
   * blue border line in it.
   * Non-private only for tests.
   */
  final static int WEST_CHROME_OFFSET_PX = 1;

  /**
   * Desired vertical space between the visual top of the popup (i.e., the blue
   * border in the north chrome image) and the bottom of the element against
   * which it is positioned.
   */
  private final static int VERTICAL_OFFSET_PX = 5;

  /**
   * Offset that shifts the alignment edge of the relative element's left
   * border-edge.  This is so that the popup may be left-aligned to some point
   * within the relative element (e.g., if the relative element is an image,
   * and the alignment point is some visual part of that image).
   */
  private final int leftAlignOffset;

  /**
   * Offset that shifts the alignment edge of the relative element's right
   * border-edge.  This is so that the popup may be right-aligned to some point
   * within the relative element (e.g., if the relative element is an image,
   * and the alignment point is some visual part of that image).
   */
  private final int rightAlignOffset;

  /**
   * A 0-0-offset instance of this strategy.
   */
  public static final AbovePopupPositioner INSTANCE = new AbovePopupPositioner(0, 0);

  /**
   * Creates a positioner.
   */
  public AbovePopupPositioner() {
    this(0, 0);
  }

  /**
   * Creates a positioner.
   *
   * @param leftAlignOffset   inwards offset (px) from the left border edge
   * @param rightAlignOffset  inwards offset (px) from the right border edge
   */
  public AbovePopupPositioner(int leftAlignOffset, int rightAlignOffset) {
    this.rightAlignOffset = rightAlignOffset;
    this.leftAlignOffset = leftAlignOffset;
  }

  /**
   * {@inheritDoc}
   *
   * @see DropdownPopupPositioner#setPopupPositionAndMakeVisible(Element, Element)
   */
  public void setPopupPositionAndMakeVisible(Element relative, Element popup) {
    // See DropdownPopupPositioner for explanation of positioning.
    int bodyWidth = RootPanel.get().getOffsetWidth();
    int bodyHeight = RootPanel.get().getOffsetHeight();
    int bodyLeft = RootPanel.get().getAbsoluteLeft();
    int bodyTop = RootPanel.get().getAbsoluteTop();
    int relLeft = relative.getAbsoluteLeft() + leftAlignOffset - bodyLeft;
    int relRight = relative.getAbsoluteRight() - rightAlignOffset - bodyLeft;
    int relTop = relative.getAbsoluteTop() - bodyTop;
    int popupWidth = popup.getOffsetWidth();

    // Will the popup fit with left-alignment (i.e., is there space on the right)?
    if (relLeft + popupWidth <= bodyWidth) {
      // Align left
      popup.getStyle().setLeft(relLeft + WEST_CHROME_OFFSET_PX, Unit.PX);
    } else {
      // Align right
      popup.getStyle().setRight(bodyWidth - relRight + EAST_CHROME_OFFSET_PX, Unit.PX);
    }
    popup.getStyle().setBottom(bodyHeight - relTop + SOUTH_CHROME_OFFSET_PX + VERTICAL_OFFSET_PX,
                               Unit.PX);

    // Finally, make the panel visible now that its position has been set.
    popup.getStyle().setVisibility(Visibility.VISIBLE);
  }
}
