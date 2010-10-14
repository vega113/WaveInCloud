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

package org.waveprotocol.box.server.robots;

import com.google.common.base.Strings;
import com.google.gxp.base.GxpContext;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.account.RobotAccountDataImpl;
import org.waveprotocol.box.server.gxp.robots.RobotRegistrationPage;
import org.waveprotocol.box.server.gxp.robots.RobotRegistrationSuccessPage;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.wave.model.id.TokenGenerator;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for Robot Registration.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotRegistrationServlet extends HttpServlet {

  private static final String CREATE_PATH = "/create";

  private static final int TOKEN_LENGTH = 48;

  private static final Log LOG = Log.get(RobotRegistrationServlet.class);

  private final AccountStore accountStore;
  private final String domain;
  private final TokenGenerator tokenGenerator;

  @Inject
  private RobotRegistrationServlet(AccountStore accountStore,
      @Named("wave_server_domain") String domain, TokenGenerator tokenGenerator) {
    this.accountStore = accountStore;
    this.domain = domain;
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String pathInfo = req.getPathInfo();
    if (CREATE_PATH.equals(pathInfo)) {
      doRegisterGet(req, resp, "");
    } else {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String pathInfo = req.getPathInfo();
    if (CREATE_PATH.equals(pathInfo)) {
      doRegisterPost(req, resp);
    } else {
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  /**
   * Handles GET request for the register page.
   *
   * @param message non-null but optional message to show on the page
   */
  private void doRegisterGet(HttpServletRequest req, HttpServletResponse resp, String message)
      throws IOException {
    RobotRegistrationPage.write(resp.getWriter(), new GxpContext(req.getLocale()), domain, message);
    resp.setContentType("text/html");
    resp.setStatus(HttpServletResponse.SC_OK);
  }

  /**
   * Handles POST request for the register page.
   */
  private void doRegisterPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String username = req.getParameter("username");
    String location = req.getParameter("location");

    if (Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(location)) {
      doRegisterGet(req, resp, "Please complete all fields.");
      return;
    }

    ParticipantId id;
    try {
      id = ParticipantId.of(username + "@" + domain);
    } catch (InvalidParticipantAddress e) {
      doRegisterGet(req, resp, "Invalid username specified, use alphanumeric characters only.");
      return;
    }

    AccountData account = accountStore.getAccount(id);
    if (account != null) {
      doRegisterGet(req, resp, username + " is already in use, please choose another one.");
      return;
    }

    URI uri;
    try {
      uri = URI.create(location);
    } catch (IllegalArgumentException e) {
      doRegisterGet(
          req, resp, "Invalid Location specified, please specify a location in URI format.");
      return;
    }

    String robotLocation = "http://" + uri.getHost() + uri.getPath();
    if (robotLocation.endsWith("/")) {
      robotLocation = robotLocation.substring(0, robotLocation.length() - 1);
    }

    // TODO(ljvderijk): Implement the verification.
    RobotAccountData robotAccount = new RobotAccountDataImpl(
        id, robotLocation, tokenGenerator.generateToken(TOKEN_LENGTH), null, true);
    accountStore.putAccount(robotAccount);
    LOG.info(robotAccount.getId() + " is now registered as a RobotAccount with Url "
        + robotAccount.getUrl());

    onRegisterSuccess(req, resp, robotAccount);
  }

  /**
   * Shows the page that signals that a robot was successfully registered a
   * robot. It will show the robot's token and token secret to use for the
   * Active API.
   *
   * @param robotAccount the newly registered robot account.
   */
  private void onRegisterSuccess(
      HttpServletRequest req, HttpServletResponse resp, RobotAccountData robotAccount)
      throws IOException {
    RobotRegistrationSuccessPage.write(resp.getWriter(), new GxpContext(req.getLocale()),
        robotAccount.getId().getAddress(), robotAccount.getConsumerSecret());
    resp.setContentType("text/html");
    resp.setStatus(HttpServletResponse.SC_OK);
  }
}