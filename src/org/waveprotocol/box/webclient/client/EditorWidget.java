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
package org.waveprotocol.box.webclient.client;

import com.google.gwt.user.client.ui.Composite;

import org.waveprotocol.box.common.DocumentConstants;
import org.waveprotocol.box.webclient.util.Log;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeProvider;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.util.ElementHandlerRegistry;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;

/**
 * A composite containing an editor and debug output.
 */
class EditorWidget extends Composite {
  @SuppressWarnings("unused")
  private static final Log LOG = Log.get(Editor.class);

  private static final String TOPLEVEL_CONTAINER_TAGNAME = DocumentConstants.BODY;

  static {
    LineContainers.setTopLevelContainerTagname(TOPLEVEL_CONTAINER_TAGNAME);
  }

  /**
   * The editor itself.
   */
  private final Editor editor;

  private final Registries testEditorRegistries = Editor.ROOT_REGISTRIES;

  public EditorWidget(DocInitialization docInit) {
    KeyBindingRegistry keysRegistry = new KeyBindingRegistry();

    ElementHandlerRegistry testHandlerRegistry = testEditorRegistries.getElementHandlerRegistry();

    LineRendering.registerContainer(TOPLEVEL_CONTAINER_TAGNAME,
        Editor.ROOT_HANDLER_REGISTRY);

    StyleAnnotationHandler.register(Editor.ROOT_REGISTRIES);

    // TODO(zamfi): add these back in if there's time to open them also.

    // FormDoodads.register(Editor.ROOT_HANDLER_REGISTRY);
    // LinkAnnotationHandler.register(Editor.ROOT_REGISTRIES,
    // new LinkAttributeAugmenter() {
    // @Override
    // public Map<String, String> augment(Map<String, Object> annotations,
    // boolean isEditing,
    // Map<String, String> current) {
    // return current;
    // }
    // });
    // Suggestion.register(Editor.ROOT_HANDLER_REGISTRY);

    editor = createEditor();
    initWidget(editor.getWidget());
    editor.init(testEditorRegistries, keysRegistry, new EditorSettings());
    editor.setContent(docInit, ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS);
    editor.setEditing(true);
  }

  public Editor getEditor() {
    return editor;
  }

  @Override
  protected void onDetach() {
    editor.cleanup();
  }

  private Editor createEditor() {
    EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
    EditorStaticDeps.setPopupChromeProvider(new PopupChromeProvider() {
      public PopupChrome createPopupChrome() {
        return null;
      }
    });
    Editor editor = Editors.create();
    return editor;
  }
}
