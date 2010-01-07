// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.examples.fedone.agents.probey;

import com.google.common.collect.ImmutableList;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.security.UserRealm;
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
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDocumentOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author arb@google.com (Anthony Baxter)
 */
public class Probey extends AbstractAgent {

  private static final Log LOG = Log.get(Probey.class);

  /**
   * Id generator used for this (server, user) pair.
   */
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
    ClientWaveView wave = getWave(waveId);
    if (wave == null) {
      return "NOT FOUND";
    }
    WaveletData convRoot = ClientUtils.getConversationRoot(wave);
    BufferedDocOp manifest = convRoot.getDocuments().get(DocumentConstants.MANIFEST_DOCUMENT_ID);
    String newDocId = getNewDocumentId();
    WaveletDelta delta = ClientUtils.createAppendBlipDelta(manifest, userId, newDocId, blipText);
    sendWaveletDelta(convRoot.getWaveletName(), delta);
    return newDocId;
  }

  /**
   * Adds a participant to a wave.
   *
   * @param waveId the ID of the wave
   * @param name   the user to add to the wave
   * @return "OK" or "NOT FOUND"
   */
  public String addUser(String waveId, String name) {
    ParticipantId addId = new ParticipantId(name);
    ClientWaveView wave = getWave(waveId);
    if (wave == null) {
      return "NOT FOUND";
    }
    WaveletData convRoot = ClientUtils.getConversationRoot(wave);
    AddParticipant addUserOp = new AddParticipant(addId);
    sendWaveletDelta(convRoot.getWaveletName(), new WaveletDelta(userId, ImmutableList.of(addUserOp)));
    return "OK";
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
  private String getBlips(String waveId) {
    ClientWaveView wave = getWave(waveId);
    if (wave == null) {
      return "NOT FOUND";
    }
    WaveletData convRoot = ClientUtils.getConversationRoot(wave);
    BufferedDocOp manifest = convRoot.getDocuments().get(DocumentConstants.MANIFEST_DOCUMENT_ID);
    final Map<String, BufferedDocOp> documentMap = ClientUtils.getConversationRoot(wave).getDocuments();
    if (manifest == null) {
      return "NO MANIFEST";
    }
    final StringBuilder builder = new StringBuilder();
    manifest.apply(new InitializationCursorAdapter((new DocInitializationCursor() {

      @Override
      public void elementStart(String type, Attributes attrs) {
        if (type.equals(DocumentConstants.BLIP)) {
          if (attrs.containsKey(DocumentConstants.BLIP_ID)) {
            BufferedDocOp document =
                documentMap.get(attrs.get(DocumentConstants.BLIP_ID));
            if (document == null) {
              // A nonexistent document is indistinguishable from the empty document, so this
              // is not necessarily an error.
            } else {

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
  public void onDocumentChanged(WaveletData wavelet, WaveletDocumentOperation documentOperation) {
    // Do nothing
  }

  @Override
  public void onParticipantAdded(WaveletData wavelet, ParticipantId participant) {
    // Do nothing
  }

  @Override
  public void onParticipantRemoved(WaveletData wavelet, ParticipantId participant) {
    // Do nothing
  }

  @Override
  public void onSelfAdded(WaveletData wavelet) {
    // Do nothing
  }

  @Override
  public void onSelfRemoved(WaveletData wavelet) {
    // Do nothing
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
          throw new IllegalArgumentException("userAtName must be in form user@domain");
        }

        Probey probey = new Probey(AgentConnection.newConnection(args[0], args[1], port),
                                   args[0]);
        Handler handler = new WebHandler(probey);
        Server server = new Server(httpport);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"probey"});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");

        HashUserRealm userRealm = new HashUserRealm();
        userRealm.setName("Probey");
        userRealm.setConfig("./etc/probey/realm.properties");
        SecurityHandler sh = new SecurityHandler();
        sh.setUserRealm(userRealm);
        sh.setConstraintMappings(new ConstraintMapping[]{cm});

        server.setUserRealms(new UserRealm[]{userRealm});
        server.setHandlers(new Handler[]{sh, handler});

        server.start();  // spawns a new thread.
        probey.run();
      } else {
        System.out
            .println("usage: java Probey <username@domain> <fedone hostname> <fedone port> <http port>");
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

    public void handle(String s, HttpServletRequest request, HttpServletResponse response, int i)
        throws IOException, ServletException {
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_OK);

      final String url = request.getRequestURI();
      if (url.equals("/new")) {
        LOG.info("creating a new wave");
        response.getWriter().println(probey.createNewWave());
      } else if (url.startsWith("/add/")) {
        String[] parts = url.split("/");
        if (parts.length != 4) {
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
          LOG.info("adding user: " + parts[3] + " to wave: " + parts[2]);
          response.getWriter().println(probey.addUser(parts[2], parts[3]));
        }
      } else if (url.startsWith("/addblip/")) {
        String[] parts = url.split("/");
        if (parts.length != 4) {
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
          LOG.info("adding blip: " + parts[3] + " to wave: " + parts[2]);
          response.getWriter().println(probey.addBlip(parts[2], parts[3]));
        }
      } else if (url.startsWith("/getblips/")) {
        String[] parts = url.split("/");
        if (parts.length != 3) {
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
          LOG.info("getting blips for wave: " + parts[2]);
          response.getWriter().println(probey.getBlips(parts[2]));
        }

      } else {
        response.getWriter().println("<h1>Unknown: " + url + "</h1>");
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
      ((Request) request).setHandled(true);
    }
  }


}
