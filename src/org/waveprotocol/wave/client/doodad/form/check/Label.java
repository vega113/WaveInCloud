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

package org.waveprotocol.wave.client.doodad.form.check;

import org.waveprotocol.wave.client.editor.NodeMutationHandler;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.Renderer;
import org.waveprotocol.wave.model.document.util.ElementHandlerRegistry;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

public class Label {
  protected static final String FULL_TAGNAME = "w:label";
  protected static final String FOR = "for";

  public static void register(ElementHandlerRegistry handlerRegistry) {
    RenderingMutationHandler renderingMutationHandler = LabelRenderingMutationHandler.getInstance();
    handlerRegistry.register(Renderer.class, FULL_TAGNAME, renderingMutationHandler);
    handlerRegistry
        .register(NodeMutationHandler.class, FULL_TAGNAME, renderingMutationHandler);
  }

  private Label() {

  }

  /**
   * @param forAttr
   * @return A content xml string containing a label
   */
  public static XmlStringBuilder constructXml(XmlStringBuilder value, String forAttr) {
    return value.wrap(FULL_TAGNAME, FOR, forAttr);
  }
}
