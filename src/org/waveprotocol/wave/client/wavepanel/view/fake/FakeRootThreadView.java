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
package org.waveprotocol.wave.client.wavepanel.view.fake;

import org.waveprotocol.wave.client.wavepanel.view.ReplyBoxView;
import org.waveprotocol.wave.client.wavepanel.view.ConversationView;
import org.waveprotocol.wave.client.wavepanel.view.RootThreadView;

/**
 * Fake, pojo implementation of a thread view.
 */
public final class FakeRootThreadView extends FakeThreadView implements RootThreadView {

  private final FakeConversationView convContainer;
  private final FakeReplyBoxView replyIndicator;

  public FakeRootThreadView(FakeConversationView container) {
    super();
    this.convContainer = container;
    this.replyIndicator = new FakeReplyBoxView(this);
  }

  @Override
  public Type getType() {
    return Type.ROOT_THREAD;
  }

  @Override
  public ConversationView getParent() {
    return convContainer;
  }

  @Override
  public void remove() {
    convContainer.remove(this);
  }

  @Override
  public String toString() {
    return "RootThread " + super.blipsToString();
  }

  @Override
  public ReplyBoxView getReplyIndicator() {
    return replyIndicator;
  }

  @Override
  public void setTotalBlipCount(int totalBlipCount) {
    // no-op
  }

  @Override
  public void setUnreadBlipCount(int unreadBlipCount) {
    // no-op
  }
}