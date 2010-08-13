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

package org.waveprotocol.wave.examples.webclient.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.examples.webclient.util.Log;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.document.operation.DocInitialization;

/**
 * Renders a single blip.
 */
class BlipView extends Composite {
  interface Binder extends UiBinder<Widget, BlipView> {
  }

  static final Binder BINDER = GWT.create(Binder.class);
  private static Log LOG = Log.get(BlipView.class);

  @UiField(provided = true)
  EditorWidget editor;

//  @UiField
//  Label insertReply;
  
  private WaveView waveView;
  private ConversationBlip blip;

  public BlipView(DocInitialization content, WaveView waveView, ConversationBlip blip) {
    editor = new EditorWidget(content);

    initWidget(BINDER.createAndBindUi(this));
    this.waveView = waveView;
    this.blip = blip;
  }

  public Editor getEditor() {
    return editor.getEditor();
  }

//  @UiHandler("insertReply")
//  void insertReply(ClickEvent e) {
//    reply(false);
//  }
//  private void reply(boolean startThread) {
//    blip.appendReplyThread();
//  }
}
