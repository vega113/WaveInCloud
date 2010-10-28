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
 *
 */

package org.waveprotocol.box.webclient.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.box.webclient.client.CcStackManager.SimpleCcDocument;
import org.waveprotocol.box.webclient.client.events.WaveOpenEvent;
import org.waveprotocol.box.webclient.client.events.WaveSelectionEventHandler;
import org.waveprotocol.box.webclient.util.Log;
import org.waveprotocol.box.webclient.waveclient.common.WaveViewServiceImpl;
import org.waveprotocol.box.webclient.waveclient.common.WebClientBackend;
import org.waveprotocol.wave.client.common.util.ClientPercentEncoderDecoder;
import org.waveprotocol.wave.concurrencycontrol.wave.CcBasedWaveView.OpenListener;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class WaveView extends Composite {
  interface Binder extends UiBinder<Widget, WaveView> {
  }

  interface Style extends CssResource {

    String blip();

    String thread();

    int threadIndent();
  }

  private static final Binder BINDER = GWT.create(Binder.class);
  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new ClientPercentEncoderDecoder());
  private static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionZeroFactoryImpl(URI_CODEC);

  private static Log LOG = Log.get(WaveView.class);

  @UiField
  Label addBlip;

  @UiField
  Button addParticipant;

  private WebClientBackend backend;

  private SimpleCcDocumentFactory docFactory;

  @UiField
  FlowPanel panel;

  @UiField
  Label participants;

  @UiField
  Style style;

  private ParticipantId participant;

  // XXX Extract impl methods to interface
  private WaveViewServiceImpl waveView;

  private final Map<String, BlipView> blipsById = new HashMap<String, BlipView>();

  private final Map<BlipView, BlipView> blipsToNext = new IdentityHashMap<BlipView, BlipView>();

  public WaveView() {
    initWidget(BINDER.createAndBindUi(this));
    if (Session.get().isLoggedIn()) {
      participant = new ParticipantId(Session.get().getAddress());
    }

    ClientEvents.get().addWaveSelectionEventHandler(
        new WaveSelectionEventHandler() {
          @Override
          public void onSelection(WaveId id) {
            setWave(id);
          }
        });
  }

  private CcStackManager manager;

  private ObservableConversationView conversationView;

  private IdGenerator idGenerator;

  public void setLegacy(WebClientBackend backend, IdGenerator idGenerator) {
    this.backend = backend;
    this.idGenerator = idGenerator;
  }

  private void setWave(final WaveId waveId) {
    LOG.info("WaveView opening wavelet " + waveId);
    docFactory = new SimpleCcDocumentFactory();
    if (this.manager != null) {
      manager.view.close();
      backend.clearWaveView(manager.getWaveId());
    }
    this.waveView = (WaveViewServiceImpl) backend.getWaveView(waveId, "",
        docFactory);
    manager = new CcStackManager(this.waveView, docFactory, HASH_FACTORY, participant);
    conversationView = WaveBasedConversationView.create(manager.view, idGenerator);
    conversationView.addListener(new ObservableConversationView.Listener() {

      @Override
      public void onConversationAdded(final ObservableConversation conversation) {
        LOG.info("XXX Saw conversation " + conversation.getId());
        conversation.addListener(new ObservableConversation.Listener() {

          private void log(String msg) {
            LOG.info("YYY saw " + msg + " on conversation " + conversation.getId());
          }

          @Override
          public void onBlipAdded(ObservableConversationBlip blip) {
            log("blip added"); // in conversation " + conversation.getId() + ": "+blip.getId());
            renderBlip(conversation, blip.getId());
          }

          @Override
          public void onBlipContentDeleted(ObservableConversationBlip blip) {
            log("blip content deleted");
          }

          @Override
          public void onBlipContentUndeleted(ObservableConversationBlip blip) {
            log("blip content undeleted");
          }

          @Override
          public void onBlipContributorAdded(ObservableConversationBlip blip,
              ParticipantId contributor) {
            log("blip contributor added");
          }

          @Override
          public void onBlipContributorRemoved(ObservableConversationBlip blip,
              ParticipantId contributor) {
            log("blip contributor removed");
          }

          @Override
          public void onBlipDeleted(ObservableConversationBlip blip) {
            log("blip deleted");
          }

          @Override
          public void onBlipSumbitted(ObservableConversationBlip blip) {
            log("blip submitted");
          }

          @Override
          public void onBlipTimestampChanged(ObservableConversationBlip blip,
              long oldTimestamp, long newTimestamp) {
            log("blip timestamp changed");
          }

          @Override
          public void onInlineThreadAdded(ObservableConversationThread thread,
              int location) {
            log("inline thread added");
          }

          @Override
          public void onParticipantAdded(ParticipantId participant) {
            String existingText = participants.getText();
            if (existingText.length() == 0) {
              participants.setText(participant.getAddress());
            } else {
              participants.setText(existingText + ", " + participant.getAddress());
            }
            log("participant added: " + participant);
          }

          @Override
          public void onParticipantRemoved(ParticipantId participant) {
            log("participant removed");
          }

          @Override
          public void onThreadAdded(ObservableConversationThread thread) {
            log("thread added");
          }

          @Override
          public void onThreadDeleted(ObservableConversationThread thread) {
            log("thread deleted");
          }

        });
        for (ObservableConversationBlip blip : conversation.getRootThread().getBlips()) {
          renderBlip(conversation, blip.getId());
        }
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (ParticipantId participant : conversation.getParticipantIds()) {
          if (! first) {
            sb.append(", ");
          } else {
            first = false;
          }
          sb.append(participant.getAddress());
        }
        participants.setText(sb.toString());
        LOG.info(conversation.getRootThread().getBlips().toString());
      }

      @Override
      public void onConversationRemoved(ObservableConversation conversation) {
        // TODO Auto-generated method stub

      }

    });
    manager.view.open(new OpenListener() {

      @Override
      public void onOpenFinished() {
        LOG.info("open finished on wave: "+waveId);
        ClientEvents.get().fireEvent(new WaveOpenEvent(waveId));
      }
    });

    panel.clear();
    blipsById.clear();
    blipsToNext.clear();

    History.newItem(waveId.serialise(), false);
  }

  public ObservableConversationView getConversationView() {
    return this.conversationView;
  }

  public CcStackManager getCcStackManager() {
    return this.manager;
  }

  private void renderBlip(ObservableConversation conversation, String blipId) {
    WaveletId waveletId = WaveletId.deserialise(conversation.getId());
    BlipView blipView = ((SimpleCcDocument) docFactory.get(waveletId, blipId)).getBlipView();
    panel.add(blipView);
    getElement().getStyle().setProperty("visibility", "visible");
    getElement().getStyle().setProperty("opacity", "1");
  }

  @UiHandler("addBlip")
  void addBlip(ClickEvent e) {
    LOG.info("asked to create new blip");
    conversationView.getRoot().getRootThread().appendBlip();
  }

  @UiHandler("addParticipant")
  void addParticipant(ClickEvent e) {
    ObservableConversation root = conversationView.getRoot();
    // TODO(arb): why is root null? ah wait, WVSI isn't getting listeners
    String id = Window.prompt("Email address?", "");
    LOG.info("adding " + id + " to wavelet " + null + " " + manager + " "
        + manager.view);
    root.addParticipant(new ParticipantId(id));
  }

  /**
   * Cooperates with BlipView
   */
  String getAuthor() {
    return participant.getAddress();
  }

  WaveViewServiceImpl getWaveViewService() {
    return waveView;
  }
}
