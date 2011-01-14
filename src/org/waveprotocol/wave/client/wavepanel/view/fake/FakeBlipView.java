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
package org.waveprotocol.wave.client.wavepanel.view.fake;

import org.waveprotocol.wave.client.common.util.LinkedSequence;
import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.BlipLinkPopupView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.InlineConversationView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationThread;

/**
 * Fake, pojo implementation of a blip view.
 *
 */
public final class FakeBlipView implements BlipView {

  private final FakeThreadView container;
  private final LinkedSequence<AnchorView> anchors = LinkedSequence.create();
  private final LinkedSequence<InlineConversationView> nested = LinkedSequence.create();
  private final FakeBlipMetaView meta = new FakeBlipMetaView(this);

  FakeBlipView(FakeThreadView container) {
    this.container = container;
  }

  @Override
  public Type getType() {
    return Type.BLIP;
  }

  @Override
  public String getId() {
    return "fakeId";
  }

  @Override
  public ThreadView getParent() {
    return container;
  }

  @Override
  public FakeBlipMetaView getMeta() {
    return meta;
  }

  @Override
  public void remove() {
    container.removeChild(this);
  }

  @Override
  public AnchorView getDefaultAnchorAfter(AnchorView ref) {
    return anchors.getNext(ref);
  }

  @Override
  public AnchorView getDefaultAnchorBefore(AnchorView ref) {
    return anchors.getPrevious(ref);
  }

  @Override
  public FakeAnchor insertDefaultAnchorBefore(AnchorView ref, ConversationThread t) {
    FakeAnchor anchor = new FakeAnchor(this);
    anchor.attach(new FakeInlineThreadView());
    anchors.insertBefore(ref, anchor);
    return anchor;
  }

  @Override
  public InlineConversationView getConversationBefore(InlineConversationView ref) {
    return nested.getPrevious(ref);
  }

  @Override
  public InlineConversationView getConversationAfter(InlineConversationView ref) {
    return nested.getNext(ref);
  }

  @Override
  public FakeInlineConversationView insertConversationBefore(InlineConversationView ref,
      Conversation conv) {
    FakeInlineConversationView convUi = new FakeInlineConversationView(this);
    nested.insertBefore(ref, convUi);
    return convUi;
  }

  void removeChild(AnchorView x) {
    anchors.remove(x);
  }

  void removeChild(BlipMetaView x) {
    throw new UnsupportedOperationException("Fakes do not support dynamic metas");
  }

  @Override
  public String toString() {
    return "Blip " + (anchors.isEmpty() ? "" : " " + anchors.toString());
  }

  @Override
  public BlipLinkPopupView createLinkPopup() {
    return new FakeBlipLinkPopupView(this);
  }
}
