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
package org.waveprotocol.wave.client.wavepanel.view.dom;

import static org.waveprotocol.wave.client.wavepanel.view.dom.DomViewHelper.load;

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.wavepanel.view.dom.full.AbstractFrameBuilder.Components;

/**
 * Dom impl of a frame.
 *
 */
public final class FrameDomImpl implements DomView {

  /** The DOM element of this view. */
  private final Element self;

  /** The HTML id of {@code self}. */
  private final String id;

  /** Element to which contents are attached. */
  private Element contentContainer;

  FrameDomImpl(Element e, String id) {
    this.self = e;
    this.id = id;
  }

  public static FrameDomImpl of(Element e) {
    return new FrameDomImpl(e, e.getId());
  }

  //
  // Structure exposed for external control.
  //

  public Element getContentContainer() {
    return contentContainer == null
        ? contentContainer = load(id, Components.CONTENTS) : contentContainer;
  }

  public void remove() {
    getElement().removeFromParent();
  }

  //
  // DomView nature.
  //

  @Override
  public Element getElement() {
    return self;
  }

  @Override
  public String getId() {
    return id;
  }

  //
  // Equality.
  //

  @Override
  public boolean equals(Object obj) {
    return DomViewHelper.equals(this, obj);
  }

  @Override
  public int hashCode() {
    return DomViewHelper.hashCode(this);
  }
}
