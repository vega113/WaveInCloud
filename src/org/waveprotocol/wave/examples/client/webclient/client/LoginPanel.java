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

package org.waveprotocol.wave.examples.client.webclient.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.examples.client.webclient.client.events.NetworkStatusEvent;
import org.waveprotocol.wave.examples.client.webclient.client.events.NetworkStatusEvent.ConnectionStatus;
import org.waveprotocol.wave.examples.client.webclient.client.events.NetworkStatusEventHandler;
import org.waveprotocol.wave.examples.client.webclient.client.events.UserLoginEvent;

public class LoginPanel extends Composite {
  interface Binder extends UiBinder<Widget, LoginPanel> {
  }

  interface Style extends CssResource {
    String bad();
    String good();
    String neutral();
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  private static final String INITIAL_NAME_FIELD_TEXT = "username";

  @UiField Label connectionStatus;
  @UiField TextBox nameField;
  @UiField Button sendButton;
  @UiField Style style;

  public LoginPanel() {
    initWidget(BINDER.createAndBindUi(this));
    ClientEvents.get().addNetworkStatusEventHandler(
        new NetworkStatusEventHandler() {
          @Override
          public void onNetworkStatus(NetworkStatusEvent event) {
            setConnectionStatus(event.getStatus());
          }
        });
    setConnectionStatus(ConnectionStatus.NEVER_CONNECTED);
    nameField.setText(INITIAL_NAME_FIELD_TEXT + "@" + Session.get().getHostname());
  }

  @UiHandler("sendButton")
  void handleSendButtonClick(ClickEvent e) {
    doLogin();
  }

  @UiHandler("nameField")
  void handleNameFieldKeyPress(KeyPressEvent e) {
    if (e.getCharCode() == '\n' || e.getCharCode() == '\r') {
      doLogin();
      e.preventDefault();
    }
  }

  public void setFocus(boolean focus) {
    if (focus) {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          if (nameField.getText().startsWith(INITIAL_NAME_FIELD_TEXT)) {
            nameField.setSelectionRange(0, INITIAL_NAME_FIELD_TEXT.length());
          } else {
            nameField.selectAll();
          }
          nameField.setFocus(true);
        }
      });
    } else {
      nameField.setFocus(false);
    }
  }

  private void doLogin() {
    String text = nameField.getText();
    if (!patternOk(text)) {
      Window.alert("You must specify an email address");
      return;
    }

    // Can't do anything until connected
    if (!sendButton.isEnabled()) {
      return;
    }
    nameField.setEnabled(false);
    nameField.setFocus(false);
    sendButton.setEnabled(false);
    ClientEvents.get().fireEvent(new UserLoginEvent(text, true));
  }

  private boolean patternOk(String text) {
    if (!text.contains("@")) {
      return false;
    }
    return true;
  }

  private void setConnectionStatus(ConnectionStatus status) {
    connectionStatus.setText(status.toString());
    connectionStatus.setStyleName(style(status));
    sendButton.setEnabled(status == ConnectionStatus.CONNECTED);
  }

  private String style(ConnectionStatus status) {
    switch (status) {
      case CONNECTED:
        return style.good();
      case NEVER_CONNECTED:
        return style.neutral();
    }
    return style.bad();
  }
}
