/**
 * Copyright 2009 Google Inc.
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

package org.waveprotocol.wave.examples.fedone.agents.probey;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.eclipse.jetty.http.security.Constraint;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.waveprotocol.wave.examples.client.common.ClientUtils;
import org.waveprotocol.wave.examples.client.common.ClientWaveView;
import org.waveprotocol.wave.examples.fedone.agents.agent.AbstractAgent;
import org.waveprotocol.wave.examples.fedone.agents.agent.AgentConnection;
import org.waveprotocol.wave.examples.fedone.common.DocumentConstants;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.examples.fedone.util.WaveletDataUtil;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.impl.InitializationCursorAdapter;
import org.waveprotocol.wave.model.id.URIEncoderDecoder;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The probey agent provides a web interface for remote applications to trigger
 * operations against a fedone waveserver. It allows easy testing of
 * Federation.
 * 
 * @author arb@google.com (Anthony Baxter)
 */
public class Probey extends AbstractAgent {

  private static final Log LOG = Log.get(Probey.class);

  /**
   * Constructor.
   *
   * @param connection the agent's connection to the server.
   */
  @Inject
  @VisibleForTesting
  Probey(AgentConnection connection) {
    super(connection);
  }

  /**
   * Adds a blip to the given wave.
   *
   * @param waveId   the ID of the wave
   * @param blipText the text to include in the blip
   * @return the new blip ID
   */
  private String addBlip(String waveId, String blipText) {
    ClientWaveView wave = getWave(WaveId.deserialise(waveId));
    if (wave == null) {
      throw new IllegalArgumentException("NOT FOUND");
    }
    WaveletData convRoot = ClientUtils.getConversationRoot(wave);
    BlipData manifest = convRoot.getDocument(DocumentConstants.MANIFEST_DOCUMENT_ID);
    String newDocId = getNewDocumentId();
    CoreWaveletDelta delta = ClientUtils.createAppendBlipDelta(manifest, getParticipantId(),
        newDocId, blipText);
    sendAndAwaitWaveletDelta(WaveletDataUtil.waveletNameOf(convRoot), delta);
    return newDocId;
  }

  /**
   * Adds a participant to a wave.
   *
   * @param waveId the ID of the wave
   * @param name   the user to add to the wave
   */
  public void addUser(String waveId, String name) {
    ParticipantId addId = ParticipantId.ofUnsafe(name);
    ClientWaveView wave = getWave(WaveId.deserialise(waveId));
    if (wave == null) {
      throw new IllegalArgumentException("NOT FOUND");
    }
    WaveletData convRoot = ClientUtils.getConversationRoot(wave);
    CoreAddParticipant addUserOp = new CoreAddParticipant(addId);
    sendAndAwaitWaveletDelta(WaveletDataUtil.waveletNameOf(convRoot),
        new CoreWaveletDelta(getParticipantId(), ImmutableList.of(addUserOp)));
  }

  /**
   * Creates a new wave.
   *
   * @return the new wave's ID
   */
  public String createNewWave() {
    ClientWaveView waveView = newWave();
    return waveView.getWaveId().serialise();
  }

  /**
   * Fetches the blips in a wave.
   *
   * @param waveId the ID of the wave
   * @return a simply formatted text version of the wave
   */
  private String renderBlips(String waveId) {
    ClientWaveView wave = getWave(WaveId.deserialise(waveId));
    if (wave == null) {
      throw new IllegalArgumentException("NOT FOUND");
    }
    final WaveletData convRoot = ClientUtils.getConversationRoot(wave);
    BlipData manifest = convRoot.getDocument(DocumentConstants.MANIFEST_DOCUMENT_ID);
    if (manifest == null) {
      throw new IllegalArgumentException("MANIFEST MISSING");
    }
    final StringBuilder builder = new StringBuilder();
    manifest.getContent().asOperation().apply(InitializationCursorAdapter.adapt((
        new DocInitializationCursor() {

          @Override
          public void elementStart(String type, Attributes attrs) {
            if (type.equals(DocumentConstants.BLIP)) {
              if (attrs.containsKey(DocumentConstants.BLIP_ID)) {
                BlipData document =
                    convRoot.getDocument(attrs.get(DocumentConstants.BLIP_ID));
                if (document != null) {
                  // A nonexistent document is indistinguishable from
                  // the empty document, so document == null is not necessarily an error.
                  builder.append("Blip: ");
                  builder.append(attrs.get(DocumentConstants.BLIP_ID));
                  builder.append("\nContent: ");
                  document.getContent().asOperation().apply(
                      InitializationCursorAdapter.adapt(new DocInitializationCursor() {
                        @Override
                        public void characters(String chars) {
                          builder.append(chars);
                        }

                        @Override
                        public void annotationBoundary(AnnotationBoundaryMap map) {
                        }

                        @Override
                        public void elementStart(String type, Attributes attrs) {
                        }

                        @Override
                        public void elementEnd() {
                        }
                      }));
                  builder.append("\n");
                }
              }
            }
          }

          @Override
          public void annotationBoundary(AnnotationBoundaryMap map) {
          }

          @Override
          public void characters(String chars) {
          }

          @Override
          public void elementEnd() {
          }
        })));
    return builder.toString();
  }

  @Override
  public void onDocumentChanged(WaveletData wavelet, String docId, BufferedDocOp docOp) {
  }

  @Override
  public void onParticipantAdded(WaveletData wavelet, ParticipantId participant) {
  }

  @Override
  public void onParticipantRemoved(WaveletData wavelet, ParticipantId participant) {
  }

  @Override
  public void onSelfAdded(WaveletData wavelet) {
  }

  @Override
  public void onSelfRemoved(WaveletData wavelet) {
  }

  public static void main(String[] args) {
    try {
      if (args.length == 4) {
        int port, httpport;
        try {
          port = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Must provide valid port.");
        }
        try {
          httpport = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Must provide valid http port.");
        }
        if (args[0].split("@").length != 2) {
          throw new IllegalArgumentException("username must be in form user@domain");
        }

        Probey probey = new Probey(AgentConnection.newConnection(args[0], args[1], port));
        Server server = new Server(httpport);

        LoginService loginService = new HashLoginService("probey","./etc/probey/realm.properties");
        //Lifecycle object that will now be started/stopped w/ server
        server.addBean(loginService); 

        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        server.setHandler(securityHandler);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"probey"});
        constraint.setAuthenticate(true);

        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint(constraint);
        constraintMapping.setPathSpec("/*");

        Set<String> knownRoles = Collections.singleton("probey");

        securityHandler.setAuthenticator(new BasicAuthenticator());
        securityHandler.setLoginService(loginService);
        securityHandler.setStrict(false);
        securityHandler.setConstraintMappings(Lists.newArrayList(constraintMapping), 
            knownRoles);

        ContextHandlerCollection contexts = new ContextHandlerCollection();

        ContextHandler context = new ContextHandler();
        context.setContextPath("/");
        context.setResourceBase(".");
        context.setClassLoader(Thread.currentThread().getContextClassLoader());
        context.setHandler(new WebHandler(probey));
        contexts.addHandler(context);

        securityHandler.setHandler(contexts);

        server.setGracefulShutdown(1000);
        server.setStopAtShutdown(true);

        server.start();  // spawns a new thread.
        probey.run();
      } else {
        System.out.println("usage: java Probey <username@domain> <fedone hostname> <fedone port>"
            + " <http port>");
      }
    } catch (Exception e) {
      LOG.severe("Catastrophic failure", e);
      System.exit(1);
    }
    System.exit(0);
  }

  static class WebHandler extends AbstractHandler {

    private final Probey probey;
    private final URIEncoderDecoder uriCodec;

    public WebHandler(Probey probey) {
      this.probey = probey;
      this.uriCodec = new URIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
        HttpServletResponse response) throws IOException {
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_OK);

      final String url = request.getRequestURI();
      final PrintWriter writer = response.getWriter();

      try {
        if (url.equals("/new")) {
          LOG.info("creating a new wave");
          final String waveId = probey.createNewWave();
          LOG.info("new wave id: " + waveId);
          writer.println(waveId);
        } else if (url.startsWith("/add/")) {
          String[] parts = url.split("/");
          if (parts.length != 4) {
            throw new IllegalArgumentException("bad request");
          }
          LOG.info("adding user: " + parts[3] + " to wave: " + parts[2]);
          probey.addUser(parts[2], parts[3]);
          writer.println("OK");

        } else if (url.startsWith("/addblip/")) {
          String[] parts = uriCodec.decode(url).split("/");
          if (parts.length != 4) {
            throw new IllegalArgumentException("bad request");
          }
          LOG.info("adding blip: " + parts[3] + " to wave: " + parts[2]);
          writer.println(probey.addBlip(parts[2], parts[3]));

        } else if (url.startsWith("/getblips/")) {
          String[] parts = uriCodec.decode(url).split("/");
          if (parts.length != 3) {
            throw new IllegalArgumentException("bad request");
          }
          LOG.info("getting blips for wave: " + parts[2]);
          writer.println(probey.renderBlips(parts[2]));

        } else {
          response.getWriter().println("Unknown: " + url);
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
      } catch (IllegalArgumentException e) {
        writer.println(e.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      } catch (EncodingException e) {
        writer.println("Invalid Encoding: " + e.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      }
      ((Request) request).setHandled(true);
    }
  }
}
