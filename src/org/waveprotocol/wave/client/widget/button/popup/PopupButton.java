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

package org.waveprotocol.wave.client.widget.button.popup;

import org.waveprotocol.wave.client.widget.button.ToggleButton;
import org.waveprotocol.wave.client.widget.button.ToggleButton.ToggleButtonListener;
import org.waveprotocol.wave.client.widget.popup.PopupEventListener;
import org.waveprotocol.wave.client.widget.popup.PopupEventSourcer;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;

/**
 * A toggle button that shows / hides a popup.
 *
 */
public class PopupButton extends ToggleButton implements ToggleButtonListener, PopupEventListener {
  private final UniversalPopup popup;
  public PopupButton(UniversalPopup popup) {
    this.popup = popup;
    setToggleButtonListener(this);
    popup.addPopupEventListener(this);
  }

  @Override
  public void onOff() {
    popup.hide();
  }

  @Override
  public void onOn() {
    popup.show();
  }

  @Override
  public void onHide(PopupEventSourcer source) {
    setIsOn(false);
  }

  @Override
  public void onShow(PopupEventSourcer source) {
    setIsOn(true);
  }
}
