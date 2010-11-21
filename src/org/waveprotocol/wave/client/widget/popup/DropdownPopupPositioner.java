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

import org.waveprotocol.wave.client.common.util.UserAgent;

/**
 * Shows the popup aligned below and flush-right against another element.
 *
 */
public class DropdownPopupPositioner implements RelativePopupPositioner {

  // TODO(user): Expose these constants on the resource bundle of the chrome images.

  /**
   * Distance from the bottom edge of the north chrome frame to the top of the
   * blue border line in it.
   */
  private final static int NORTH_CHROME_OFFSET_PX = 4;

  /**
   * Distance from the left edge of the east chrome frame to the right of the
   * blue border line in it.
   */
  private final static int EAST_CHROME_OFFSET_PX = 1;

  /**
   * Distance from the right edge of the west chrome frame to the left of the
   * blue border line in it.
   */
  private final static int WEST_CHROME_OFFSET_PX = 1;

  /**
   * Singleton instance of this strategy.
   */
  public static final DropdownPopupPositioner INSTANCE = new DropdownPopupPositioner();

  /**
   * Desired vertical space between the visual top of the popup (i.e., the blue
   * border in the north chrome image) and the bottom of the element against
   * which it is positioned.
   *
   * Defaults to 9 (5 plus default desktop chrome top offset) pixels.
   */
  private int verticalOffsetPx = 5 + NORTH_CHROME_OFFSET_PX;

  /**
   * Desired horizontal space between the left side of the popup and the right
   * side of the element which it is to positioned.
   *
   * Defaults to 0.
   */
  private int horizontalOffsetPx = 0;

  public DropdownPopupPositioner withVerticalOffsetPx(int px) {
    this.verticalOffsetPx = px;
    return this;
  }

  public DropdownPopupPositioner withHorizontalOffsetPx(int px) {
    this.horizontalOffsetPx = px;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  public void setPopupPositionAndMakeVisible(Element relative, Element popup) {
    //
    // The box from which the absolute positions of elements are measured is different on different
    // browsers, due to the use of margins on the body element (NOTE: the "absolute positions" of an
    // element, as reported by getAbsoluteX, are the positions of its border edge; i.e., adding
    // margins to an element changes its "absolute position", but padding and borders do not).
    //
    // IE measures absolute offsets from the top-left of the entire window:
    //  __________________________________
    // |          |         |             |
    // |         top        |             |
    // |        __V_________|__________   |
    // |-left->|            | top      |  |
    // |       |        ____V____      |  |
    // |-----left----->|_________|     |  |
    // |-----------------right-->      |  |
    // |       |                       |  |
    // |----------------------right--->|  |
    // |       |_______________________|  |
    // |                                  |
    // |__________________________________|
    //
    // Safari measures the positions from the top-left of the body's border edge
    // (i.e., body margins do not affect top-left positions of anything, but body borders do).
    //  ____________________________________
    // |   ______________________________   |
    // |  |                   | top      |  |
    // |  |               ____V____      |  |
    // |  |----left----->|_________|     |  |
    // |  |-----------------right->      |  |
    // |  |______________________________|  |
    // |____________________________________|
    //
    // We treat the body's border edge as the constraining frame, so therefore we normalize the
    // reported positions by subtracting the top/left position of the body element.
    // Note that the position of the popup is set using absolute positioning, which positions the
    // entire popup box (i.e., its margin edge) within the context of the body element's padding
    // edge.  We hope that nobody puts padding on the body, so we can use the border-edge as the
    // positioning context.
    //

    int bodyWidth = RootPanel.get().getOffsetWidth();
    int bodyLeft = RootPanel.get().getAbsoluteLeft();
    int bodyTop = RootPanel.get().getAbsoluteTop();
    int relLeft = relative.getAbsoluteLeft() - bodyLeft + horizontalOffsetPx;
    int relRight = relative.getAbsoluteRight() - bodyLeft - horizontalOffsetPx;
    int relBot = relative.getAbsoluteBottom() - bodyTop + verticalOffsetPx;
    if (UserAgent.isWebkit()) {
      // Webkit is off by 1 pixel.
      relBot -= 1;
    }
    int popupWidth = popup.getOffsetWidth();
    int popupHeight = popup.getOffsetHeight();

    // Will the popup fit with right-alignment (i.e., is there space on the left)?
    if (popupWidth <= relRight) {
      // Align to the right
      popup.getStyle().setRight(bodyWidth - relRight + EAST_CHROME_OFFSET_PX,
          Unit.PX);
      popup.getStyle().clearLeft();
    } else {
      // Align to the left
      popup.getStyle().setLeft(relLeft + WEST_CHROME_OFFSET_PX, Unit.PX);
      popup.getStyle().clearRight();
    }

    int top = relBot;
    top = PositionUtil.boundToScreenVertical(top, popupHeight);
    popup.getStyle().setTop(top, Unit.PX);

    // Finally, make the panel visible now that its position has been set.
    popup.getStyle().setVisibility(Visibility.VISIBLE);
  }
}
