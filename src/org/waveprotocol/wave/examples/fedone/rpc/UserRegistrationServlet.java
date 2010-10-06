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

import com.google.gxp.base.GxpContext;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.waveprotocol.wave.examples.fedone.account.HumanAccountDataImpl;
import org.waveprotocol.wave.examples.fedone.authentication.HttpRequestBasedCallbackHandler;
import org.waveprotocol.wave.examples.fedone.authentication.PasswordDigest;
import org.waveprotocol.wave.examples.fedone.gxp.UserRegistrationPage;
import org.waveprotocol.wave.examples.fedone.persistence.AccountStore;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Locale;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The user registration servlet allows new users to register accounts.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class UserRegistrationServlet extends HttpServlet {
  private AccountStore accountStore;
  private String domain;

  @Inject
  public UserRegistrationServlet(
      AccountStore accountStore, @Named("wave_server_domain") String domain) {
    this.accountStore = accountStore;
    this.domain = domain;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    writeRegistrationPage("", req.getLocale(), resp);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    BufferedReader body = req.getReader();
    MultiMap<String> parameters = new UrlEncoded(body.readLine());

    try {
      String username = parameters.getString(HttpRequestBasedCallbackHandler.ADDRESS_FIELD);
      String password = parameters.getString(HttpRequestBasedCallbackHandler.PASSWORD_FIELD);

      tryCreateUser(username, password, req.getLocale(), resp);
    } finally {
      body.close();
    }
  }

  // Returns an error message on error, null on success.
  private void tryCreateUser(
      String username, String password, Locale locale, HttpServletResponse response)
      throws IOException {
    String message = null;
    ParticipantId id = null;

    try {
      // First, some cleanup on the parameters.
      username = username.trim();
      if (username.contains(ParticipantId.DOMAIN_PREFIX)) {
        id = ParticipantId.of(username);
      } else {
        id = ParticipantId.of(username + ParticipantId.DOMAIN_PREFIX + domain);
      }
      if (id.getAddress().indexOf("@") < 2) {
        message = "Username portion of address cannot be less than 2 characters";
      }
    } catch (InvalidParticipantAddress e) {
      message = "Invalid username";
    }

    if (message == null && accountStore.getAccount(id) != null) {
      message = "Account already exists";
    }

    if (message != null) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    } else {
      if (password == null) {
        // Register the user with an empty password.
        password = "";
      }

      HumanAccountDataImpl account =
          new HumanAccountDataImpl(id, new PasswordDigest(password.toCharArray()));
      accountStore.putAccount(account);
      message = "Registered " + id.getAddress();
      response.setStatus(HttpServletResponse.SC_OK);
    }

    writeRegistrationPage(message, locale, response);
  }

  private void writeRegistrationPage(String message, Locale locale, HttpServletResponse dest)
      throws IOException {
    dest.setContentType("text/html");
    UserRegistrationPage.write(dest.getWriter(), new GxpContext(locale), domain, message);
  }
}
