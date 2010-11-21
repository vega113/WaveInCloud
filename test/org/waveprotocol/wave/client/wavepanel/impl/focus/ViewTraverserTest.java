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
 */
package org.waveprotocol.wave.client.wavepanel.impl.focus;


import junit.framework.TestCase;

import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ConversationView;
import org.waveprotocol.wave.client.wavepanel.view.InlineConversationView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeAnchor;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeBlipView;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeConversationView;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeInlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeThreadView;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeTopConversationView;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Tests the traversal ordering.
 *
 */

public class ViewTraverserTest extends TestCase {

  /**
   * Knows how to populate the structure of, and verify the travesral order of,
   * threads in a blip.
   */
  static class BlipBuilder {
    private final ThreadBuilder[] anchored;
    private final ThreadBuilder[] unanchored;
    private final ConversationBuilder[] privates;

    BlipBuilder(ThreadBuilder[] anchored, ThreadBuilder[] unanchored,
        ConversationBuilder[] privates) {
      this.anchored = anchored;
      this.unanchored = unanchored;
      this.privates = privates;
    }

    void populate(FakeBlipView blip) {
      BlipMetaView meta = blip.getMeta();
      for (ThreadBuilder threadBuilder : unanchored) {
        threadBuilder.populate(blip.insertDefaultAnchorBefore(null, null).getThread());
      }
      for (ThreadBuilder threadBuilder : anchored) {
        FakeAnchor anchor = blip.insertDefaultAnchorBefore(null, null);
        FakeInlineThreadView thread = anchor.getThread();
        threadBuilder.populate(thread);
        anchor.detach(thread);
        blip.getMeta().createInlineAnchorBefore(null).attach(thread);
      }
      for (ConversationBuilder conversationBuilder : privates) {
        conversationBuilder.populate(blip.insertConversationBefore(null, null));
      }
    }

    void verify(Queue<BlipView> blips, BlipView blip) {
      assertEquals(blips.poll(), blip);
      BlipMetaView meta = blip.getMeta();

      AnchorView a = meta.getInlineAnchorAfter(null);
      for (ThreadBuilder threadBuilder : anchored) {
        assertNotNull(a);
        threadBuilder.verify(blips, a.getThread());
        a = meta.getInlineAnchorAfter(a);
      }
      assertNull(a);

      int anchoredDefaults = 0;  // empty default anchors.
      a = blip.getDefaultAnchorAfter(null);
      for (ThreadBuilder threadBuilder : unanchored) {
        assertNotNull(a);
        InlineThreadView thread = a.getThread();
        while (thread == null) {
          a = blip.getDefaultAnchorAfter(a);
          thread = a.getThread();
          assertNotNull(a);
          anchoredDefaults++;
        }
        threadBuilder.verify(blips, thread);
        a = blip.getDefaultAnchorAfter(a);
      }
      while (a != null) {
        assertNull(a.getThread());
        a = blip.getDefaultAnchorAfter(a);
        anchoredDefaults++;
      }
      assertNull(a);
      assertEquals(anchored.length, anchoredDefaults);

      InlineConversationView c = blip.getConversationAfter(null);
      for (ConversationBuilder conversationBuilder : privates) {
        assertNotNull(c);
        conversationBuilder.verify(blips, c);
        c = blip.getConversationAfter(c);
      }
      assertNull(c);
    }
  }

  /**
   * Knows how to populate the structure of, and verify the travesral order of,
   * blips in a thread.
   */
  static class ThreadBuilder {
    private final BlipBuilder[] blipBuilders;

    ThreadBuilder(BlipBuilder... blipBuilders) {
      this.blipBuilders = blipBuilders;
    }

    void populate(FakeThreadView thread) {
      for (BlipBuilder blipBuilder : blipBuilders) {
        blipBuilder.populate(thread.insertBlipBefore(null, null));
      }
    }

    void verify(Queue<BlipView> blips, ThreadView thread) {
      BlipView blip = thread.getBlipAfter(null);
      for (BlipBuilder blipBuilder : blipBuilders) {
        assertNotNull(blip);
        blipBuilder.verify(blips, blip);
        blip = thread.getBlipAfter(blip);
      }
      assertNull(blip);
    }
  }

  /**
   * Knows how to populate the structure of, and verify the travesral order of,
   * an inline conversation.
   */
  static class ConversationBuilder {
    private final ThreadBuilder rootBuilder;

    ConversationBuilder(ThreadBuilder rootBuilder) {
      this.rootBuilder = rootBuilder;
    }

    void populate(FakeConversationView conversation) {
      rootBuilder.populate(conversation.getRootThread());
    }

    void verify(Queue<BlipView> blips, ConversationView conversation) {
      rootBuilder.verify(blips, conversation.getRootThread());
    }
  }

  /** Traverser to test. */
  private ViewTraverser traverser;

  /** View to build and traverse. */
  private FakeTopConversationView c;

  private static ConversationBuilder createSimpleSample() {
    return conversation(
        thread(
            leafBlip(),
            blip(
                anchored(
                    thread(leafBlip())
                ),
                unanchored(
                    thread(leafBlip(), leafBlip()),
                    thread(leafBlip())
                ),
                privates(
                    conversation(thread(leafBlip())),
                    conversation(thread(leafBlip()))
                )
            )
        )
    );
  }

  private static ConversationBuilder createEmptyThreadSample() {
    return conversation(
        thread(
            leafBlip(),
            blip(
                anchored(thread()),
                unanchored(
                    thread(
                        leafBlip(),
                        leafBlip()
                    ),
                    thread()
                ),
                privates(
                    conversation((thread(leafBlip()))),
                    conversation((thread())),
                    conversation((thread(leafBlip())))
                )
            )
        )
    );
  }

  private static ConversationBuilder createComplexSample() {
    return conversation(
        thread(
            leafBlip(),
            blip(
                anchored(
                    thread()
                ),
                anchored(
                    thread(
                        leafBlip(),
                        leafBlip()
                    ),
                    thread(
                        leafBlip()
                    )
                )
            ),
            blip(
                anchored(
                    thread(
                        leafBlip(),
                        leafBlip()
                    ),
                    thread(
                        leafBlip()
                    )
                ),
                unanchored(),
                privates(
                    conversation(
                        thread(
                            leafBlip(),
                            blip(
                                anchored(
                                    thread()
                                ),
                                anchored(
                                    thread(
                                        leafBlip(),
                                        leafBlip()
                                    ),
                                    thread(
                                        leafBlip()
                                    )
                                )
                            ),
                            blip(
                                anchored(),
                                unanchored(),
                                privates(
                                    conversation(
                                        thread(
                                            leafBlip(),
                                            leafBlip()
                                        )
                                    )
                                )
                            ),
                            leafBlip()
                        )
                    ),
                    conversation(
                        thread(
                            leafBlip(),
                            leafBlip()
                        )
                    )
                )
            ),
            blip(
                anchored(),
                anchored(
                    thread(
                        blip(
                            anchored(),
                            unanchored(
                                thread(
                                    leafBlip(),
                                    leafBlip()
                                )
                            )
                        )
                    )
                )
            ),
            leafBlip()
        )
    );
  }

  static BlipBuilder leafBlip() {
    return blip(anchored(), unanchored(), privates());
  }

  static ThreadBuilder [] anchored(ThreadBuilder ... threads) {
    return threads;
  }

  static ThreadBuilder [] unanchored(ThreadBuilder ... threads) {
    return threads;
  }

  static ConversationBuilder [] privates(ConversationBuilder ... threads) {
    return threads;
  }

  static BlipBuilder blip(ThreadBuilder[] anchored, ThreadBuilder[] unanchored,
      ConversationBuilder ... privates) {
    return new BlipBuilder(anchored, unanchored, privates);
  }

  static ThreadBuilder thread(BlipBuilder ... blips) {
    return new ThreadBuilder(blips);
  }

  static ConversationBuilder conversation(ThreadBuilder root) {
    return new ConversationBuilder(root);
  }

  @Override
  protected void setUp() {
    traverser = new ViewTraverser();
    c = new FakeTopConversationView();
  }

  public void testSimpleForward() {
    ConversationBuilder sample = createSimpleSample();
    sample.populate(c);
    sample.verify(getOrderViaForward(c), c);
  }

  public void testSimpleReverse() {
    ConversationBuilder sample = createSimpleSample();
    sample.populate(c);
    sample.verify(getOrderViaReverse(c), c);
  }

  public void testSomeEmptyForward() {
    ConversationBuilder sample = createEmptyThreadSample();
    sample.populate(c);
    sample.verify(getOrderViaForward(c), c);
  }

  public void testSomeEmptyReverse() {
    ConversationBuilder sample = createEmptyThreadSample();
    sample.populate(c);
    sample.verify(getOrderViaReverse(c), c);
  }

  public void testComplexForward() {
    ConversationBuilder sample = createComplexSample();
    sample.populate(c);
    sample.verify(getOrderViaForward(c), c);
  }

  public void testReverseTraversalOrder() {
    ConversationBuilder sample = createComplexSample();
    sample.populate(c);
    sample.verify(getOrderViaReverse(c), c);
  }

  private LinkedList<BlipView> getOrderViaForward(ConversationView c) {
    LinkedList<BlipView> list = new LinkedList<BlipView>();
    for (BlipView b = traverser.getFirst(c); b != null; b = traverser.getNext(b)) {
      list.add(b);
    }
    return list;
  }

  private LinkedList<BlipView> getOrderViaReverse(ConversationView c) {
    LinkedList<BlipView> list = new LinkedList<BlipView>();
    for (BlipView b = traverser.getLast(c); b != null; b = traverser.getPrevious(b)) {
      list.add(b);
    }
    Collections.reverse(list);
    return list;
  }
}
