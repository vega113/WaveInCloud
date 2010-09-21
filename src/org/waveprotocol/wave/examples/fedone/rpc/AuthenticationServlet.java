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

package org.waveprotocol.wave.examples.fedone.rpc;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.waveprotocol.wave.examples.fedone.authentication.AccountStorePrincipal;
import org.waveprotocol.wave.examples.fedone.authentication.ConfigurationProvider;
import org.waveprotocol.wave.examples.fedone.authentication.HttpRequestBasedCallbackHandler;
import org.waveprotocol.wave.examples.fedone.util.Log;

import java.io.IOException;
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
 * A servlet for authenticating a user's password and giving them a token
 * via a cookie.
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class AuthenticationServlet extends HttpServlet {
  // TODO(josephg): Make this pretty and put it somewhere else. The login
  //     page should be implemented with GWT as part of the client.
  private static final String SIMPLE_AUTH_FORM =
    "<html><body><form name=\"auth\" method=\"POST\">"
    + "<table class=\"form\"><tr><td>Username (foo@example.com)</td><td>"
    + "<input name=\"username\" /></td></tr><tr><td>Password</td><td><input"
    + " name=\"password\" type=\"password\" /></td></tr><tr><td colspan="
    + "\"3\"><input type=\"submit\" value=\"Login\" /><br /></table></form>"
    + "</body></html>";
  
  private static final Log LOG = Log.get(AuthenticationServlet.class);
  
  private Configuration configuration;
  
  @Inject
  public AuthenticationServlet(Configuration configuration) {
    this.configuration = configuration;
  }

  private Subject login(MultiMap<String> parameters) throws LoginException {
    Subject subject = new Subject();
    CallbackHandler callbackHandler = new HttpRequestBasedCallbackHandler(parameters);
    
    LoginContext context = new LoginContext(ConfigurationProvider.CONTEXT_NAME,
        subject, callbackHandler, configuration);
    
    // If authentication fails, login() will throw a LoginException.
    context.login();
    return subject;
  }
  
  /**
   * The POST request should have all the fields required for authentication.
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    Preconditions.checkNotNull(configuration);
    
    MultiMap<String> arguments = new MultiMap<String>();
    UrlEncoded.decodeUtf8To(req.getInputStream(), arguments, 1024);
    
    Subject subject;
    try {
      subject = login(arguments);
    } catch (LoginException e) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      LOG.info("User authentication failed: " + e.getLocalizedMessage());
      return;
    }
    
    String loggedInUser = getLoggedInUsername(subject);
    if (loggedInUser == null) {
      throw new IllegalStateException("The user provided valid authentication"
          + " information, but we don't know how to map their identity to a"
          + " wave username.");
    }
    
    HttpSession session = req.getSession(true);
    session.setAttribute("username", loggedInUser);
    session.setAttribute("subject", subject);
    
    // TODO(josephg): Redirect back to where the user was last.
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/plain");
    resp.getWriter().println("Authenticated.");
  }
  
  String getLoggedInUsername(Subject subject) {
    for (Principal p : subject.getPrincipals()) {
      // TODO(josephg): When we support other authentication types (LDAP, etc),
      // this method will need to read the username portion out of the other
      // principal types.
      if (p instanceof AccountStorePrincipal) {
        return ((AccountStorePrincipal) p).getName();
      }
    }
    
    return null;
  }
  
  /**
   * On GET, present a login form.
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    Preconditions.checkNotNull(configuration);
    
    resp.setStatus(HttpServletResponse.SC_OK);
    resp.setContentType("text/html");

    HttpSession session = req.getSession(false);
    if (session != null) {
      String username = (String) session.getAttribute("username");
      resp.getWriter().print("<html><body>Already authenticated as "
          + username + "</body></html>");
    } else {
      resp.getWriter().write(SIMPLE_AUTH_FORM);
    }
  }
}
