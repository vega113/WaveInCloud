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

import com.google.common.collect.ImmutableList;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.security.*;
import org.waveprotocol.wave.examples.fedone.agents.agent.AbstractAgent;
import org.waveprotocol.wave.examples.fedone.agents.agent.AgentConnection;
import org.waveprotocol.wave.examples.fedone.common.DocumentConstants;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientUtils;
import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientWaveView;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.impl.InitializationCursorAdapter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDocumentOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * The probey agent provides a web interface for remote applications to trigger operations against a
 * fedone waveserver. It allows easy testing of Federation.
 *
 * @author arb@google.com (Anthony Baxter)
 */
public class Probey extends AbstractAgent {

  private static final Log LOG = Log.get(Probey.class);

  private ParticipantId userId;

  /**
   * Constructor.
   *
   * @param connection   the agent's connection to the server.
   * @param userAtDomain the user@domain name of the robot
   */
  private Probey(AgentConnection connection, String userAtDomain) {
    super(connection);
    this.userId = new ParticipantId(userAtDomain);
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
    BufferedDocOp manifest = convRoot.getDocuments().get(DocumentConstants.MANIFEST_DOCUMENT_ID);
    String newDocId = getNewDocumentId();
    WaveletDelta delta = ClientUtils.createAppendBlipDelta(manifest, userId, newDocId, blipText);
    sendAndAwaitWaveletDelta(convRoot.getWaveletName(), delta);
    return newDocId;
  }

  /**
   * Adds a participant to a wave.
   *
   * @param waveId the ID of the wave
   * @param name   the user to add to the wave
   */
  public void addUser(String waveId, String name) {
    ParticipantId addId = new ParticipantId(name);
    ClientWaveView wave = getWave(WaveId.deserialise(waveId));
    if (wave == null) {
      throw new IllegalArgumentException("NOT FOUND");
    }
    WaveletData convRoot = ClientUtils.getConversationRoot(wave);
    AddParticipant addUserOp = new AddParticipant(addId);
    sendAndAwaitWaveletDelta(convRoot.getWaveletName(), new WaveletDelta(userId,
                                                                         ImmutableList.of(
                                                                             addUserOp)));
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
    WaveletData convRoot = ClientUtils.getConversationRoot(wave);
    BufferedDocOp manifest = convRoot.getDocuments()
        .get(DocumentConstants.MANIFEST_DOCUMENT_ID);
    final Map<String, BufferedDocOp> documentMap =
        ClientUtils.getConversationRoot(wave).getDocuments();
    if (manifest == null) {
      throw new IllegalArgumentException("MANIFEST MISSING");
    }
    final StringBuilder builder = new StringBuilder();
    manifest.apply(new InitializationCursorAdapter((
        new DocInitializationCursor() {

          @Override
          public void elementStart(String type, Attributes attrs) {
            if (type.equals(DocumentConstants.BLIP)) {
              if (attrs.containsKey(DocumentConstants.BLIP_ID)) {
                BufferedDocOp document =
                    documentMap.get(attrs.get(DocumentConstants.BLIP_ID));
                if (document != null) {
                  // A nonexistent document is indistinguishable from
                  // the empty document, so document == null is not necessarily an error.
                  builder.append("Blip: ");
                  builder.append(attrs.get(DocumentConstants.BLIP_ID));
                  builder.append("\nContent: ");
                  document.apply(new InitializationCursorAdapter(new DocInitializationCursor() {

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
  public void onDocumentChanged(WaveletData wavelet,
                                WaveletDocumentOperation documentOperation) {
  }

  @Override
  public void onParticipantAdded(WaveletData wavelet,
                                 ParticipantId participant) {
  }

  @Override
  public void onParticipantRemoved(WaveletData wavelet,
                                   ParticipantId participant) {
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

        Probey probey = new Probey(AgentConnection.newConnection(args[0],
                                                                 args[1],
                                                                 port),
                                   args[0]);
        Handler handler = new WebHandler(probey);
        Server server = new Server(httpport);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"probey"});
        constraint.setAuthenticate(true);

        ConstraintMapping constraintMapping = new ConstraintMapping();
        constraintMapping.setConstraint(constraint);
        constraintMapping.setPathSpec("/*");

        HashUserRealm userRealm = new HashUserRealm();
        userRealm.setName("Probey");
        userRealm.setConfig("./etc/probey/realm.properties");
        SecurityHandler securityHandler = new SecurityHandler();
        securityHandler.setUserRealm(userRealm);
        securityHandler.setConstraintMappings(new ConstraintMapping[]{constraintMapping});

        server.setUserRealms(new UserRealm[]{userRealm});
        server.setHandlers(new Handler[]{securityHandler, handler});

        server.start();  // spawns a new thread.
        probey.run();
      } else {
        System.out
            .println("usage: java Probey <username@domain> <fedone hostname>"
                     + " <fedone port> <http port>");
      }
    } catch (Exception e) {
      LOG.severe("Catastrophic failure", e);
      System.exit(1);
    }
    System.exit(0);
  }

  static class WebHandler extends AbstractHandler {

    private Probey probey;

    public WebHandler(Probey probey) {
      this.probey = probey;
    }

    @Override
    public void handle(String s, HttpServletRequest request, HttpServletResponse response, int i)
        throws IOException, ServletException {
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
          String[] parts = url.split("/");
          if (parts.length != 4) {
            throw new IllegalArgumentException("bad request");
          }
          LOG.info("adding blip: " + parts[3] + " to wave: " + parts[2]);
          writer.println(probey.addBlip(parts[2], parts[3]));

        } else if (url.startsWith("/getblips/")) {
          String[] parts = url.split("/");
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
      }
      ((Request) request).setHandled(true);
    }
  }
}
