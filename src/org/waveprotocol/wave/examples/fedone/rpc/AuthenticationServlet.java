/**
 * Copyright 2010 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.waveprotocol.wave.examples.fedone.rpc;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.waveprotocol.wave.examples.fedone.authentication.HttpRequestBasedCallbackHandler;
import org.waveprotocol.wave.examples.fedone.authentication.ParticipantPrincipal;
import org.waveprotocol.wave.examples.fedone.authentication.SessionManager;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.security.Principal;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * A servlet for authenticating a user's password and giving them a token via a
 * cookie.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class AuthenticationServlet extends HttpServlet {
  // TODO(josephg): Make this pretty and put it somewhere else. The login
  // page should be implemented with GWT as part of the client.
  private static final String SIMPLE_AUTH_FORM = "<html><body><form name=\"auth\" method=\"POST\">"
      + "<table class=\"form\"><tr><td>Username (foo@example.com)</td><td>" + "<input name=\""
      + HttpRequestBasedCallbackHandler.ADDRESS_FIELD
      + "\" /></td></tr><tr><td>Password</td><td><input name=\""
      + HttpRequestBasedCallbackHandler.PASSWORD_FIELD
      + "\" type=\"password\" /></td></tr><tr><td colspan=\"3\">"
      + "<input type=\"submit\" value=\"Login\" /><br /></table></form></body></html>";

  private static final Log LOG = Log.get(AuthenticationServlet.class);

  private final Configuration configuration;
  private final SessionManager sessionManager;

  @Inject
  public AuthenticationServlet(Configuration configuration, SessionManager sessionManager) {
    Preconditions.checkNotNull(configuration, "Configuration is null");
    Preconditions.checkNotNull(sessionManager, "Session manager is null");
    this.configuration = configuration;
    this.sessionManager = sessionManager;
  }

  /**
   * This method is used to read the arguments out of the request body. Care is
   * taken to make sure the password is never stored in a string.
   */
  @SuppressWarnings("unchecked")
  private MultiMap<String> getArgumentMap(BufferedReader body) throws IOException {
    // TODO(josephg): Figure out a way to do this encoding without using
    // intermediate string representations of the password.
    return new UrlEncoded(body.readLine());
  }

  private LoginContext login(BufferedReader body) throws IOException, LoginException {
    Subject subject = new Subject();
    CallbackHandler callbackHandler = new HttpRequestBasedCallbackHandler(getArgumentMap(body));

    LoginContext context = new LoginContext(
        "Wave", subject, callbackHandler, configuration);

    // If authentication fails, login() will throw a LoginException.
    context.login();
    return context;
  }

  /**
   * The POST request should have all the fields required for authentication.
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    LoginContext context;
    try {
      context = login(req.getReader());
    } catch (LoginException e) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      LOG.info("User authentication failed: " + e.getLocalizedMessage());
      return;
    }

    Subject subject = context.getSubject();

    ParticipantId loggedInAddress;
    try {
      loggedInAddress = getLoggedInUser(subject);
    } catch (InvalidParticipantAddress e1) {
      throw new IllegalStateException(
          "The user provided valid authentication information, but the username"
              + " isn't a valid user address.");
    }

    if (loggedInAddress == null) {
      try {
        context.logout();
      } catch (LoginException e) {
        // Logout failed. Absorb the error, since we're about to throw an
        // illegal state exception anyway.
      }
      throw new IllegalStateException(
          "The user provided valid authentication information, but we don't "
              + "know how to map their identity to a wave user address.");
    }

    HttpSession session = req.getSession(true);
    sessionManager.setLoggedInUser(session, loggedInAddress);
    // The context needs to be notified when the user logs out.
    session.setAttribute("context", context);
    LOG.info("Authenticated user " + loggedInAddress);

    // If the user specified a redirect location (/auth?r=/some/other/place)
    // then redirect them to that URL.
    String query = req.getQueryString();
    
    if (query != null && query.startsWith("r=")) {
      String encoded_url = query.substring("r=".length());
      String url = URLDecoder.decode(encoded_url, "UTF-8");
      resp.sendRedirect(url);
    } else {
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("text/plain");
      resp.getWriter().println("Authenticated as " + loggedInAddress);
    }
  }

  /**
   * Get the participant id of the given subject.
   *
   *  The subject is searched for compatible principals. When other
   * authentication types are added, this method will need to be updated to
   * support their principal types.
   *
   * @throws InvalidParticipantAddress The subject's address is invalid
   */
  private ParticipantId getLoggedInUser(Subject subject) throws InvalidParticipantAddress {
    String address = null;

    for (Principal p : subject.getPrincipals()) {
      // TODO(josephg): When we support other authentication types (LDAP, etc),
      // this method will need to read the address portion out of the other
      // principal types.
      if (p instanceof ParticipantPrincipal) {
        address = ((ParticipantPrincipal) p).getName();
        break;
      }
    }

    return address == null ? null : ParticipantId.of(address);
  }

  /**
   * On GET, present a login form if the user isn't authenticated.
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/html");

    HttpSession session = req.getSession(false);
    ParticipantId user = sessionManager.getLoggedInUser(session);

    if (user != null) {
      resp.getWriter().print("<html><body>Already authenticated as " + user + "</body></html>");
    } else {
      resp.getWriter().write(SIMPLE_AUTH_FORM);
    }
  }
}
