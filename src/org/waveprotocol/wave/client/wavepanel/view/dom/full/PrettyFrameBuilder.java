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
package org.waveprotocol.wave.client.wavepanel.view.dom.full;

import static org.waveprotocol.wave.client.uibuilder.OutputHelper.append;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.close;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.open;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;

import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;

/**
 * A UiBuilder that builds the external wave panel chrome.
 *
 */
public final class PrettyFrameBuilder extends AbstractFrameBuilder {

  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {
    /** CSS */
    @Source("Frame.css")
    Css css();

    /** Default background images */
    @Source("panel_n.png")
    @ImageOptions(repeatStyle=RepeatStyle.Horizontal)
    ImageResource chromeNorth();

    @Source("panel_ne.png")
    ImageResource chromeNorthEast();

    @Source("panel_e.png")
    @ImageOptions(repeatStyle=RepeatStyle.Vertical)
    ImageResource chromeEast();

    @Source("panel_se.png")
    ImageResource chromeSouthEast();

    @Source("panel_s.png")
    @ImageOptions(repeatStyle=RepeatStyle.Horizontal)
    ImageResource chromeSouth();

    @Source("panel_sw.png")
    ImageResource chromeSouthWest();

    @Source("panel_w.png")
    @ImageOptions(repeatStyle=RepeatStyle.Vertical)
    ImageResource chromeWest();

    @Source("panel_nw.png")
    ImageResource chromeNorthWest();
  }

  /** CSS for this widget. */
  public interface Css extends CssResource {
    // chrome names
    String north();
    String northEast();
    String east();
    String southEast();
    String south();
    String southWest();
    String west();
    String northWest();

    /** Whole frame. */
    String frame();

    /** Element containing the content. */
    String contentContainer();
  }

  public final static Resources RESOURCES = GWT.create(Resources.class);
  public final static Css CSS = RESOURCES.css();

  static {
    StyleInjector.inject(CSS.getText(), true);
  }

  PrettyFrameBuilder(String id, UiBuilder contents) {
    super(id, contents);
  }

  public static PrettyFrameBuilder create(String id, UiBuilder contents) {
    return new PrettyFrameBuilder(id, contents);
  }

  @Override
  public void outputHtml(SafeHtmlBuilder out) {
    open(out, id, CSS.frame(), TypeCodes.kind(Type.FRAME));
      // <!-- chrome div elements -->
      append(out, null, CSS.north(), null);
      append(out, null, CSS.northEast(), null);
      append(out, null, CSS.north(), null);
      append(out, null, CSS.east(), null);
      append(out, null, CSS.southEast(), null);
      append(out, null, CSS.south(), null);
      append(out, null, CSS.southWest(), null);
      append(out, null, CSS.west(), null);
      append(out, null, CSS.northWest(), null);
      open(out, Components.CONTENTS.getDomId(id), CSS.contentContainer(), null);
        contents.outputHtml(out);
      close(out);
    close(out);
  }
}
