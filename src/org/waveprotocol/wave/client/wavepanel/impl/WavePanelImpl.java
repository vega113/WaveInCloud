/**
 * Copyright 2010 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.waveprotocol.wave.client.wavepanel.impl;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.KeyCombo;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.scroll.TargetScroller;
import org.waveprotocol.wave.client.wavepanel.WavePanel;
import org.waveprotocol.wave.client.wavepanel.event.EventDispatcherPanel;
import org.waveprotocol.wave.client.wavepanel.event.EventHandlerRegistry;
import org.waveprotocol.wave.client.wavepanel.event.Focusable;
import org.waveprotocol.wave.client.wavepanel.event.KeySignalRouter;
import org.waveprotocol.wave.client.wavepanel.view.FrameView;
import org.waveprotocol.wave.client.wavepanel.view.TopConversationView;
import org.waveprotocol.wave.client.wavepanel.view.View;
import org.waveprotocol.wave.client.wavepanel.view.dom.DomAsViewProvider;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * A wave panel, which is just a view and an event system.
 *
 */
public final class WavePanelImpl implements WavePanel, Focusable, SourcesEvents<
    WavePanelImpl.LifecycleListener> {

  public interface LifecycleListener {
    void onInit();

    void onReset();
  }

  /** Event system on which features install their controllers. */
  private final EventDispatcherPanel panel;

  /** Key handlers for key events that are routed to this panel. */
  private final KeySignalRouter keys = new KeySignalRouter();

  /** Views through which features manipulate the UI. */
  private final DomAsViewProvider views;

  private final CopyOnWriteSet<LifecycleListener> listeners = CopyOnWriteSet.create();

  //
  // Fields referencing the wave rendering are dynamically inserted and removed.
  //

  /** Frame inside the panel. */
  private FrameView<? extends TopConversationView> frame;

  // Cached references.

  /** Main conversation shown in this panel. */
  private TopConversationView main;

  /** The target scroller used to scroll a view into the viewport. */
  private TargetScroller<? super View> scroller;

  private WavePanelImpl(
      DomAsViewProvider views, EventDispatcherPanel panel) {
    this.views = views;
    this.panel = panel;
  }

  /**
   * Creates a wave panel.
   *
   * @param views view bundle
   * @param panelDom element in the DOM on which to build the wave panel
   * @param container panel to adopt the wave panel's widget, or {@code null}
   *        for the wave panel to be a root widget
   */
  public static WavePanelImpl create(
      DomAsViewProvider views, Element panelDom, LogicalPanel container) {
    Preconditions.checkArgument(panelDom != null);
    EventDispatcherPanel events =
        (container != null) ? EventDispatcherPanel.inGwtContext(panelDom, container)
            : EventDispatcherPanel.of(panelDom);
    WavePanelImpl panel = new WavePanelImpl(views, events);

    // Existing content?
    Element frameDom = panelDom.getFirstChildElement();
    if (frameDom != null) {
      panel.init(frameDom);
    }
    return panel;
  }

  /**
   * Destroys this wave panel, releasing its resources.
   */
  public void destroy() {
    panel.removeFromParent();
  }

  /** @return the scroller for bringing views in the root thread into view. */
  public TargetScroller<? super View> getScroller() {
    Preconditions.checkState(frame != null);
    // Java's type inference is so bad that even (p ? x : x) fails to compile,
    // because the compiler treats the two x captures as not substitutable.
    // Therefore, this method can not be implemented with a ternary.
    if (scroller == null) {
      scroller = getMain().getScroller();
    }
    return scroller;
  }


  /**
   * This method is intended to be visible only to subpackages.
   *
   * @return the provider of views of dom elements.
   */
  public DomAsViewProvider getViewProvider() {
    return views;
  }

  /**
   * This method is intended to be visible only to subpackages.
   *
   * @return the registry against which event handlers are registered.
   */
  public EventHandlerRegistry getHandlers() {
    return panel;
  }

  /**
   * This method is intended to be visible only to subpackages.
   *
   * @return the panel for connecting GWT widgets.
   */
  public LogicalPanel getGwtPanel() {
    return panel;
  }

  /**
   * This method is intended to be visible only to subpackages.
   *
   * @return the registry of key handlers for when focus is on the wave panel.
   */
  public KeySignalRouter getKeyRouter() {
    return keys;
  }

  /**
   * This method is intended to be visible only to subpackages.
   *
   * @return the UI of the main conversation in this panel.
   */
  public TopConversationView getMain() {
    Preconditions.checkState(frame != null);
    return main == null ? main = frame.getContents() : main;
  }

  /** @return true if this panel has contents. */
  public boolean hasContents() {
    return frame != null;
  }

  //
  // Key plumbing.
  //

  @Override
  public boolean onKeySignal(KeyCombo key) {
    return keys.onKeySignal(key);
  }

  @Override
  public void onFocus() {
  }

  @Override
  public void onBlur() {
  }

  //
  // Lifecycle.
  //

  public void init(Element frame) {
    Preconditions.checkState(this.frame == null);
    Preconditions.checkArgument(frame != null);
    panel.getElement().appendChild(frame);
    this.frame = views.asFrame(frame);
    fireOnInit();
  }

  public void reset() {
    Preconditions.checkState(frame != null);
    frame.remove();
    frame = null;
    main = null;
    scroller = null;
    fireOnReset();
  }

  @Override
  public void addListener(LifecycleListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(LifecycleListener listener) {
    listeners.remove(listener);
  }

  private void fireOnInit() {
    for (LifecycleListener listener : listeners) {
      listener.onInit();
    }
  }

  private void fireOnReset() {
    for (LifecycleListener listener : listeners) {
      listener.onReset();
    }
  }
}
