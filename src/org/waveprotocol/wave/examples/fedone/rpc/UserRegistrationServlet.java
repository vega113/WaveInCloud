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

import org.waveprotocol.wave.examples.fedone.account.HumanAccountDataImpl;
import org.waveprotocol.wave.examples.fedone.authentication.HttpRequestBasedCallbackHandler;
import org.waveprotocol.wave.examples.fedone.authentication.PasswordDigest;
import org.waveprotocol.wave.examples.fedone.gxp.UserRegistrationPage;
import org.waveprotocol.wave.examples.fedone.persistence.AccountStore;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

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
public final class UserRegistrationServlet extends HttpServlet {
  private final AccountStore accountStore;
  private final String domain;

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
    String message = tryCreateUser(req.getParameter(HttpRequestBasedCallbackHandler.ADDRESS_FIELD),
        req.getParameter(HttpRequestBasedCallbackHandler.PASSWORD_FIELD));

    if (message != null) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
    } else {
      message = "Registration complete.";
      resp.setStatus(HttpServletResponse.SC_OK);
    }

    writeRegistrationPage(message, req.getLocale(), resp);
  }
  
  /**
   * Try to create a user with the provided username and password. On error,
   * returns a string containing an error message. On success, returns null.
   */
  private String tryCreateUser(String username, String password) {
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
        return "Username portion of address cannot be less than 2 characters";
      }
    } catch (InvalidParticipantAddress e) {
      return "Invalid username";
    }

    if (accountStore.getAccount(id) != null) {
      return "Account already exists";
    }

    if (password == null) {
      // Register the user with an empty password.
      password = "";
    }

    HumanAccountDataImpl account =
        new HumanAccountDataImpl(id, new PasswordDigest(password.toCharArray()));
    accountStore.putAccount(account);

    return null;
  }

  private void writeRegistrationPage(String message, Locale locale, HttpServletResponse dest)
      throws IOException {
    dest.setContentType("text/html");
    UserRegistrationPage.write(dest.getWriter(), new GxpContext(locale), domain, message);
  }
}
