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
 */
package org.waveprotocol.wave.client.wavepanel.view.dom.full;

import org.waveprotocol.wave.client.uibuilder.UiBuilder;

/**
 * A utility class that contains the ViewFactory for a default client and the mobile client.
 *
 */
public final class ViewFactories {

  private ViewFactories() {
  }

  private static abstract class BaseFactory implements ViewFactory {
    @Override
    public final InlineConversationViewBuilder createInlineConversationView(String id,
        UiBuilder threadUi, UiBuilder participantsUi) {
      return InlineConversationViewBuilder.create(id, participantsUi, threadUi);
    }
  }

  /**
   * A ViewFactory that creates views for the desktop client.  It has a
   * chrome frame and a scrollbar for the top conversation.
   */
  public static final ViewFactory DEFAULT = new BaseFactory() {
    @Override
    public UiBuilder createFrame(String id, UiBuilder contents) {
      return PrettyFrameBuilder.create(id, contents);
    }

    @Override
    public TopConversationViewBuilder createTopConversationView(String id, UiBuilder threadUi,
        UiBuilder participantsUi) {
      return ScrollableConversationViewBuilder.createRoot(id, threadUi, participantsUi);
    }
  };

  /**
   * A ViewFactory that creates views for the mobile client.  It has a
   * no chrome frame and no scroll.
   */
  public static final ViewFactory MOBILE = new BaseFactory() {
    @Override
    public UiBuilder createFrame(String id, UiBuilder contents) {
      return SimpleFrameBuilder.create(id, contents);
    }

    @Override
    public TopConversationViewBuilder createTopConversationView(String id, UiBuilder threadUi,
        UiBuilder participantsUi) {
      return SimpleConversationViewBuilder.createRoot(id, threadUi, participantsUi);
    }
  };
}
