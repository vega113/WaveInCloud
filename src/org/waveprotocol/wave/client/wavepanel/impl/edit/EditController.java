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
package org.waveprotocol.wave.client.wavepanel.impl.edit;

import org.waveprotocol.wave.client.common.util.KeyCombo;
import org.waveprotocol.wave.client.wavepanel.event.KeySignalHandler;
import org.waveprotocol.wave.client.wavepanel.event.KeySignalRouter;
import org.waveprotocol.wave.client.wavepanel.impl.edit.Actions.Action;

import java.util.EnumMap;
import java.util.Map;

/**
 * Defines the UI actions that can be performed as part of the editing feature.
 * This includes editing, replying, and deleting blips in a conversation.
 *
 */
public final class EditController implements KeySignalHandler {

  /** Action performer. */
  private final Actions actions;

  private final EnumMap<KeyCombo, Action> keyBindings =
      new EnumMap<KeyCombo, Action>(KeyCombo.class);

  private static final EnumMap<KeyCombo, Action> DEFAULT_BINDINGS =
      new EnumMap<KeyCombo, Action>(KeyCombo.class);

  static {
    DEFAULT_BINDINGS.put(KeyCombo.CTRL_E, Action.EDIT_BLIP);
    DEFAULT_BINDINGS.put(KeyCombo.CTRL_R, Action.REPLY_TO_BLIP);
    DEFAULT_BINDINGS.put(KeyCombo.CTRL_ENTER, Action.REPLY_TO_BLIP);
    DEFAULT_BINDINGS.put(KeyCombo.ENTER, Action.REPLY_TO_BLIP);
    DEFAULT_BINDINGS.put(KeyCombo.SHIFT_ENTER, Action.CONTINUE_THREAD);
    DEFAULT_BINDINGS.put(KeyCombo.DELETE, Action.DELETE_BLIP);
  }

  EditController(Actions actions) {
    this.actions = actions;
  }

  public static EditController create(Actions actions, KeySignalRouter router) {
    EditController controller = new EditController(actions);
    controller.install(DEFAULT_BINDINGS, router);
    return controller;
  }

  private void install(Map<KeyCombo, Action> keyBindings, KeySignalRouter router) {
    this.keyBindings.putAll(keyBindings);
    router.register(keyBindings.keySet(), this);
  }

  @Override
  public boolean onKeySignal(KeyCombo key) {
    Action action = keyBindings.get(key);
    return action != null ? doAction(action) : false;
  }

  private boolean doAction(Action action) {
    assert action != null;
    switch (action) {
      case EDIT_BLIP:
        actions.startEditing();
        break;
      case REPLY_TO_BLIP:
        actions.reply();
        break;
      case DELETE_BLIP:
        actions.deleteBlip();
        break;
      case CONTINUE_THREAD:
        actions.addContinuation();
        break;
      case DELETE_THREAD:
        actions.deleteThread();
        break;
      default:
        throw new AssertionError("unknown action: " + action);
    }
    return true;
  }
}
