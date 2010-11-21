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

import static org.waveprotocol.wave.client.uibuilder.BuilderHelper.nonNull;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.appendSpan;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.close;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.closeSpan;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.open;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.openSpan;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.openSpanWith;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.uibuilder.BuilderHelper.Component;
import org.waveprotocol.wave.client.uibuilder.HtmlClosure;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;

/**
 * Holds resources and view-building logic for a collapsible callout box.
 *
 */
public final class CollapsibleBuilder implements UiBuilder {
  /** Name of the attribute that stores collapsed state (must be HTML safe). */
  public final static String COLLAPSED_ATTRIBUTE = "c";

  /** {@link #COLLAPSED_ATTRIBUTE}'s value for collapsed (must be HTML safe). */
  public final static String COLLAPSED_VALUE = "c";

  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {
    @Source("Collapsible.css")
    Css css();

    @Source("callout.png")
    ImageResource callout();

    @Source("expanded.png")
    ImageResource expanded();

    @Source("collapsed.png")
    ImageResource collapsed();
  }

  /** CSS class names for this widget. */
  public interface Css extends CssResource {
    String toggle();

    String chrome();

    String dropContainer();

    String drop();

    String collapsed();

    String expanded();
  }

  public enum Components implements Component {
    /** The toggle button. */
    TOGGLE("T"),
    /** The chrome element, also the container for contents. */
    CHROME("C"),
    /** The container of the callout triangle. */
    DROP_CONTAINER("D"), ;

    private final String suffix;

    Components(String suffix) {
      this.suffix = suffix;
    }

    @Override
    public String getDomId(String baseId) {
      return baseId + suffix;
    }

    /** @return the base id of a DOM id for this component. */
    public String getBaseId(String domId) {
      Preconditions.checkArgument(domId.endsWith(suffix), "Not a toggle id: ", domId);
      return domId.substring(0, domId.length() - suffix.length());
    }
  }

  /**
   * A unique id for this builder.
   */
  private final String id;
  private final Css css;

  //
  // Intrinsic state.
  //

  private boolean collapsed;

  //
  // Structural components.
  //

  private final HtmlClosure content;

  private final String kind;

  /**
   */
  public static CollapsibleBuilder create(String id, String kind, HtmlClosure contents) {
    // must not contain ', it is especially troublesome because it cause
    // security issues.
    Preconditions.checkArgument(!id.contains("\'"));
    return new CollapsibleBuilder(
        id, nonNull(contents), WavePanelResourceLoader.getCollapsible().css(), kind);
  }

  @VisibleForTesting
  CollapsibleBuilder(String id, HtmlClosure content, Css css, String kind) {
    this.id = id;
    this.content = content;
    this.css = css;
    this.kind = kind;
  }

  public void setCollapsed(boolean collapsed) {
    this.collapsed = collapsed;
  }

  public boolean isCollapsed() {
    return collapsed;
  }

  @Override
  public void outputHtml(SafeHtmlBuilder output) {
    //
    // <span thread>
    // <span toggle expanded>
    // <span dropContainer expanded>
    // <span drop/>
    // </span>
    // </span>
    // <div chrome expanded>
    // ...
    // </div>
    //
    String stateCss = " " + (collapsed ? css.collapsed() : css.expanded());
    String extra = collapsed ? COLLAPSED_ATTRIBUTE + "='" + COLLAPSED_VALUE + "'" : "";
    openSpanWith(output, id, null, kind, extra);
    openSpan(output, Components.TOGGLE.getDomId(id), css.toggle() + stateCss,
        TypeCodes.kind(Type.TOGGLE));
    openSpan(output, Components.DROP_CONTAINER.getDomId(id), css.dropContainer() + stateCss, null);
    appendSpan(output, null, css.drop(), null);
    closeSpan(output);
    closeSpan(output);
    open(output, Components.CHROME.getDomId(id), css.chrome() + stateCss, null);
    content.outputHtml(output);
    close(output);
    closeSpan(output);
  }

}
