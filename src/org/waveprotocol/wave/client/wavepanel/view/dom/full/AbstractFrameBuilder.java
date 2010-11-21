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

import org.waveprotocol.wave.client.uibuilder.BuilderHelper.Component;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;

/**
 * A UiBuilder that builds a frame around another.
 *
 */
public abstract class AbstractFrameBuilder implements UiBuilder {

  public enum Components implements Component {
    /** Element to which contents are attached. */
    CONTENTS("C")
    ;

    private final String postfix;

    Components(String postfix) {
      this.postfix = postfix;
    }

    @Override
    public String getDomId(String baseId) {
      return baseId + postfix;
    }
  }

  protected final String id;
  protected final UiBuilder contents;

  AbstractFrameBuilder(String id, UiBuilder contents) {
    this.id = id;
    this.contents = contents;
  }
}
