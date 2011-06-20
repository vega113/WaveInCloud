/**
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
 */
package org.waveprotocol.box.server.rpc.render.view.builder;

import static org.waveprotocol.box.server.rpc.render.uibuilder.OutputHelper.append;
import static org.waveprotocol.box.server.rpc.render.uibuilder.OutputHelper.close;
import static org.waveprotocol.box.server.rpc.render.uibuilder.OutputHelper.openWith;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.waveprotocol.box.server.rpc.render.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.box.server.rpc.render.uibuilder.UiBuilder;
import org.waveprotocol.box.server.rpc.render.view.IntrinsicContinuationIndicatorView;
import org.waveprotocol.box.server.rpc.render.view.TypeCodes;
import org.waveprotocol.box.server.rpc.render.view.View.Type;
import org.waveprotocol.box.server.rpc.render.view.builder.WavePanelResources.WaveImageResource;

/**
 * This class is the view builder for the inline continuation indicator.
 */
public final class ContinuationIndicatorViewBuilder implements UiBuilder,
    IntrinsicContinuationIndicatorView {

  public interface Resources {
    Css css();
    WaveImageResource continuationIcon();
  }

  public interface Css {
    String indicator();
    String icon();
    String bar();
  }
  
  /** A unique id for this builder. */
  private final String id;

  /** The css resources for the builder. */
  private final Css css;

  //
  // Intrinsic state.
  //

  private boolean enabled = true;

  /**
   * Creates a new reply box view builder with the given id.
   *
   * @param id unique id for this builder, it must only contains alphanumeric
   *        characters
   */
  public static ContinuationIndicatorViewBuilder create(WavePanelResources resources, String id) {
    return new ContinuationIndicatorViewBuilder(resources.getContinuationIndicator().css(), id);
  }

  @VisibleForTesting
  ContinuationIndicatorViewBuilder(Css css, String id) {
    // must not contain ', it is especially troublesome because it cause
    // security issues.
    Preconditions.checkArgument(!id.contains("\'"));
    this.css = css;
    this.id = id;
  }

  //
  // DomImpl nature.
  //

  @Override
  public void outputHtml(SafeHtmlBuilder output) {
    openWith(output, id, css.indicator(), TypeCodes.kind(Type.CONTINUATION_INDICATOR),
        enabled ? "" : "style='display:none'");
    {
      append(output, null, css.icon(), null);
      append(output, null, css.bar(), null);
    }
    close(output);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void enable() {
    setEnabled(true);
  }

  @Override
  public void disable() {
    setEnabled(false);
  }
  
  private void setEnabled( boolean enabled ) {
    this.enabled = enabled;
  }
}