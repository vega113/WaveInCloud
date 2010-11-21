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
package org.waveprotocol.wave.client.wavepanel.view.dom;

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.scroll.SmartScroller;
import org.waveprotocol.wave.client.scroll.TargetScroller;

/**
 * The Dom Impl for the scrollable conversation.
 *
 */
public final class ScrollableConversationDomImpl extends TopConversationDomImpl {
  /**
   * @param e
   * @param id
   */
  ScrollableConversationDomImpl(Element e, String id) {
    super(e, id);
  }

  public static TopConversationDomImpl of(Element e) {
    return new ScrollableConversationDomImpl(e, e.getId());
  }

  @Override
  public TargetScroller<? super Element> getScroller() {
    return SmartScroller.create(getThreadContainer());
  }
}
