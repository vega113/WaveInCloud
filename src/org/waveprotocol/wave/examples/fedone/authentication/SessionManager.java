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

package org.waveprotocol.wave.examples.fedone.authentication;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.waveprotocol.wave.examples.fedone.account.AccountData;
import org.waveprotocol.wave.examples.fedone.persistence.AccountStore;
import org.waveprotocol.wave.model.wave.ParticipantId;

import javax.servlet.http.HttpSession;

/**
 * Utility class for managing the authentication information stored in the
 * client's session object.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public final class SessionManager {
  private static final String USER_FIELD = "user";

  private final AccountStore accountStore;

  @Inject
  public SessionManager(AccountStore accountStore) {
    this.accountStore = accountStore;
  }

  /**
   * Get the participant id of the currently logged in user from the user's HTTP
   * session.
   *
   *  If the session is null, or if the user is not logged in, this function
   * returns null.
   *
   * @param session The user's HTTP session, usually obtained from
   *        request.getSession(false);
   * @return the user's participant id, or null if the user is not logged in.
   */
  public ParticipantId getLoggedInUser(HttpSession session) {
    if (session != null) {
      return (ParticipantId) session.getAttribute(USER_FIELD);
    } else {
      return null;
    }
  }

  /**
   * Get account data of the currently logged in user.
   *
   *  If the session is null, or if the user is not logged in, this function
   * returns null.
   *
   * @param session The user's HTTP session, usually obtained from
   *        request.getSession(false);
   * @return the user's account data, or null if the user is not logged in.
   */
  public AccountData getLoggedInAccount(HttpSession session) {
    // Consider caching the account data in the session object.
    ParticipantId user = getLoggedInUser(session);
    if (user != null) {
      return accountStore.getAccount(user);
    } else {
      return null;
    }
  }

  /**
   * Bind the user's participant id to the user's session.
   *
   * This records that a user has been logged in.
   *
   * @param session The user's HTTP session, usually obtained from
   *        request.getSession(true);
   * @param id the user who has been logged in
   */
  public void setLoggedInUser(HttpSession session, ParticipantId id) {
    Preconditions.checkNotNull(session, "Session is null");
    Preconditions.checkNotNull(id, "Participant id is null");
    session.setAttribute(USER_FIELD, id);
  }

  /**
   * Log the user out.
   *
   * If session is null, this function has no effect.
   *
   * @param session The user's HTTP session, obtainable from
   *        request.getSession(false);
   */
  public void logout(HttpSession session) {
    if (session != null) {
      // This function should also remove any other bound fields in the session
      // object.
      session.removeAttribute(USER_FIELD);
    }
  }
}
