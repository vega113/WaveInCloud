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

package org.waveprotocol.wave.examples.client.webclient.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;

import org.waveprotocol.wave.client.Stages;
import org.waveprotocol.wave.client.util.ClientFlags;
import org.waveprotocol.wave.client.widget.common.ImplPanel;
import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.wave.CcBasedWavelet;
import org.waveprotocol.wave.examples.client.webclient.client.events.NetworkStatusEvent;
import org.waveprotocol.wave.examples.client.webclient.client.events.NetworkStatusEvent.ConnectionStatus;
import org.waveprotocol.wave.examples.client.webclient.client.events.UserLoginEvent;
import org.waveprotocol.wave.examples.client.webclient.client.events.UserLoginEventHandler;
import org.waveprotocol.wave.examples.client.webclient.client.events.WaveCreationEvent;
import org.waveprotocol.wave.examples.client.webclient.client.events.WaveCreationEventHandler;
import org.waveprotocol.wave.examples.client.webclient.client.events.WaveIndexUpdatedEvent;
import org.waveprotocol.wave.examples.client.webclient.client.events.WaveSelectionEvent;
import org.waveprotocol.wave.examples.client.webclient.client.events.WaveUpdatedEvent;
import org.waveprotocol.wave.examples.client.webclient.util.Log;
import org.waveprotocol.wave.examples.client.webclient.waveclient.common.WaveViewServiceImpl;
import org.waveprotocol.wave.examples.client.webclient.waveclient.common.WebClientBackend;
import org.waveprotocol.wave.examples.client.webclient.waveclient.common.WebClientUtils;
import org.waveprotocol.wave.examples.common.SessionConstants;
import org.waveprotocol.wave.examples.fedone.waveserver.ProtocolSubmitResponse;
import org.waveprotocol.wave.examples.fedone.waveserver.ProtocolWaveletUpdate;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.id.IdFilters;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class WebClient implements EntryPoint {
  interface Binder extends UiBinder<DockLayoutPanel, WebClient> {
  }

  interface Style extends CssResource {
    String subPanel();
    String waveView();
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField
  SplitLayoutPanel splitPanel;

  @UiField
  Style style;

  @UiField
  ImplPanel contentPanel;

  static Log LOG = Log.get(WebClient.class);

  private WebClientBackend backend = null;

  /** The old wave panel */
  private WaveView waveView = null;

  /**
   * Create a remote websocket to talk to the server-side FedOne service.
   */
  private WaveWebSocketClient websocket;

  private ParticipantId loggedInUser;

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @Override
      public void onUncaughtException(Throwable e) {
        LOG.severe("Unhandled exception", e);
      }
    });

    // Set up UI
    RootPanel.get("app").add(BINDER.createAndBindUi(this));

    ClientEvents.get().addUserLoginEventHandler(new UserLoginEventHandler() {
      @Override
      public void onUserLoginRequest(UserLoginEvent event) {
        loginToServer(event.getUsername());
      }
      @Override
      public void onUserLoginSuccess(UserLoginEvent event) {
        loggedInUser = new ParticipantId(event.getUsername());

        hackSetSessionData(SessionConstants.ADDRESS, event.getUsername());
      }

      // HACK(zdwang/gentle): Remove this when the proper login flow is installed
      // where the user has to be logged in before being able to see the client.
      // In the proper flow the session data will be filled by the
      // server rather than having to fill it in here.
      private native String hackSetSessionData(String key, String value) /*-{
        return $wnd.__session[key] = value;
      }-*/;

    });

    websocket = new WaveWebSocketClient(new WaveWebSocketCallback() {

      public void connected() {
        ClientEvents.get().fireEvent(
            new NetworkStatusEvent(ConnectionStatus.CONNECTED));
      }

      public void disconnected() {
        ClientEvents.get().fireEvent(
            new NetworkStatusEvent(ConnectionStatus.DISCONNECTED));
      }

      public void handleWaveletUpdate(final ProtocolWaveletUpdate message) {
        LOG.info("Wavelet update for " + message.getWaveletName());
        // Pass to ClientBackend
        if (backend != null) {
          LOG.info("handling wavelet update");
          backend.receiveWaveletUpdate(message);
        }
      }

      public void receiveSubmitResponse(final ProtocolSubmitResponse message,
          int sequenceNumber) {
        // To change body of implemented methods use File | Settings | File
        // Templates.
      }

      public void unknown() {
        // TODO(arb): implement
      }
    });

    if (!ClientFlags.get().enableWavePanelHarness()) {
      waveView = new WaveView();
      contentPanel.add(waveView);

      // DockLayoutPanel manually set position relative. We need to clear it.
      waveView.getElement().getStyle().clearPosition();
    }

    ClientEvents.get().addWaveCreationEventHandler(
        new WaveCreationEventHandler() {

          @Override
          public void onCreateRequest(WaveCreationEvent event) {

            if (ClientFlags.get().enableWavePanelHarness()) {
              Stages stages = new StageOneProvider(
                  contentPanel.getElement().appendChild(Document.get().createDivElement()),
                  contentPanel);
              stages.load(null);
            } else {
              LOG.info("WaveCreationEvent received");
              if (backend == null) {
                LOG.info("Not creating wave since there is no backend");
                return;
              }

              final WaveId newWaveId = backend.getIdGenerator().newWaveId();

              ClientEvents.get().fireEvent(new WaveSelectionEvent(newWaveId));
  //            ClientEvents.get().addWaveOpenEventHandler(new WaveOpenEventHandler() {
  //
  //              @Override
  //              public void onOpen(WaveId id) {
  //                LOG.info("created the conversation root, whee!");
  //              }
  //            });
              ObservableConversation convo = waveView.getConversationView().createRoot();
              CcBasedWavelet rootWavelet = waveView.getCcStackManager().view.getRoot();
              rootWavelet.addParticipant(loggedInUser);
              LOG.info("created conversation: " + convo);
              convo.getRootThread().appendBlip();


  //            SimpleCcDocumentFactory docFactory = new SimpleCcDocumentFactory();
  //            final CcStackManager mgr = new CcStackManager(
  //                (WaveViewServiceImpl) backend.getWaveView(newWaveId, "",
  //                    docFactory), docFactory, backend.getUserId());
  //            mgr.view.open(new OpenListener() {
  //              @Override
  //              public void onOpenFinished() {
  //                LOG.info("Wave open; creating wavelet " + newWaveId);
  //                CcBasedWavelet root = mgr.view.createRoot();
  //                LOG.info("wavelet created");
  //                // mgr.view.close();
  //                ClientEvents.get().fireEvent(
  //                    new WaveSelectionEvent(mgr.view.getWaveId()));
  //              }
  //            });
            }
          }
        });

    HistorySupport.init();

    websocket.connect(getWebSocketBaseUrl(GWT.getModuleBaseURL()) + "socket");

    // For some reason, if this code is executed immediately (without the timer), the backend isn't
    // initialized properly.
    // TODO(josephg): Refactor this, potentially removing UserLoginEvent.
    new Timer() {
      @Override
      public void run() {
        String address = Session.get().getAddress();
        if (address != null) {
          LOG.info("User logged in");
          ClientEvents.get().fireEvent(new UserLoginEvent(address, true));
        }
      }
    }.schedule(100);
    
    LOG.info("SimpleWebClient.onModuleLoad() done");
  }

  /**
   * Returns <code>ws://yourhost[:port]/</code>.
   */
  // XXX check formatting wrt GPE
  private native String getWebSocketBaseUrl(String moduleBase) /*-{return "ws" + /:\/\/[^\/]+/.exec(moduleBase)[0] + "/";}-*/;

  /**
   * Send the name from the nameField to the server and wait for a response.
   */
  private void loginToServer(final String userInput) {
    backend = new WebClientBackend(userInput, websocket);

    if (!ClientFlags.get().enableWavePanelHarness()) {
      waveView.setBackend(backend);
    }

    SimpleCcDocumentFactory docFactory = new SimpleCcDocumentFactory();
    final WaveViewServiceImpl indexWave = (WaveViewServiceImpl) backend.getIndexWave(docFactory);
    indexWave.viewOpen(IdFilters.ALL_IDS, null,
        new WaveViewService.OpenCallback() {

          @Override
          public void onException(ChannelException e) {
            LOG.severe("ChannelException opening index wave", e);
            ClientEvents.get().fireEvent(new UserLoginEvent(userInput, e));
          }

          @Override
          public void onFailure(String reason) {
            LOG.info("Failure for index wave " + reason);
            ClientEvents.get().fireEvent(
                new UserLoginEvent(userInput, new RuntimeException(reason)));
          }

          @Override
          public void onSuccess(String response) {
            LOG.info("Success for index wave subscription");
            ClientEvents.get().fireEvent(new UserLoginEvent(userInput, false));
          }

          @Override
          public void onUpdate(WaveViewService.WaveViewServiceUpdate update) {
            LOG.info("IndexWave update received hasDeltas="
                + update.hasDeltas() + "  hasWaveletSnapshot="
                + update.hasWaveletSnapshot());
            ClientEvents.get().fireEvent(
                new WaveUpdatedEvent(indexWave, update.getChannelId(),
                    update.getWaveletId()));
            ClientEvents.get().fireEvent(
                new WaveIndexUpdatedEvent(
                    WebClientUtils.getIndexEntries(indexWave)));
          }
        });
  }
}
