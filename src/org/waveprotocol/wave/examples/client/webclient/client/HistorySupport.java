/*
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

package org.waveprotocol.wave.examples.client.webclient.client;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;

import org.waveprotocol.wave.examples.client.webclient.client.events.UserLoginEvent;
import org.waveprotocol.wave.examples.client.webclient.client.events.UserLoginEventHandler;
import org.waveprotocol.wave.examples.client.webclient.client.events.WaveSelectionEvent;
import org.waveprotocol.wave.examples.client.webclient.client.events.WaveSelectionEventHandler;
import org.waveprotocol.wave.examples.client.webclient.util.Log;
import org.waveprotocol.wave.model.id.WaveId;

/**
 * Contains the code to interface the history event mechanism with the client's
 * event bus. At the moment, a history token simply encodes a wave id.
 */
public class HistorySupport {
  private static final Log LOG = Log.get(HistorySupport.class);

  public static void init() {
    History.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> event) {
        String encodedToken = event.getValue();
        if (encodedToken == null || encodedToken.length() == 0) {
          return;
        }

        WaveId id = WaveId.deserialise(encodedToken);
        LOG.info("Changing selected wave based on history event to "
            + id.getId());
        ClientEvents.get().fireEvent(new WaveSelectionEvent(id));
      }
    });

    ClientEvents.get().addWaveSelectionEventHandler(
        new WaveSelectionEventHandler() {
          @Override
          public void onSelection(WaveId id) {
            String encoded = id.serialise();
            History.newItem(encoded, false);
            LOG.info("Added new history token state");
          }
        });
  }

  private HistorySupport() {
  }
}
