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

import javax.servlet.http.HttpSession;

/**
 * Utility class for managing the authentication information stored in the
 * client's session object.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class SessionManager {
  private static final String ADDRESS_FIELD = "address";

  private final AccountStore accountStore;

  @Inject
  public SessionManager(AccountStore accountStore) {
    this.accountStore = accountStore;
  }

  /**
   * Get the address of the currently logged in user from the user's session.
   *
   *  If the session has not been created, or if the user is not logged in, this
   * function returns null.
   *
   * @param session The user's HTTP session, obtainable from
   *        request.getSession(false);
   * @return the user's address, or null if the user is not logged in.
   */
  public String getLoggedInAddress(HttpSession session) {
    if (session != null) {
      return (String) session.getAttribute(ADDRESS_FIELD);
    } else {
      return null;
    }
  }

  /**
   * Get the currently logged in user's account data.
   *
   *  If the session has not been created, or if the user is not logged in, this
   * function returns null.
   *
   * @param session The user's HTTP session, obtainable from
   *        request.getSession(false);
   * @return the user's account data, or null if the user is not logged in.
   */
  public AccountData getLoggedInUser(HttpSession session) {
    // Consider caching the account data in the session object.
    String address = getLoggedInAddress(session);
    if (address != null) {
      return accountStore.getAccount(address);
    } else {
      return null;
    }
  }

  /**
   * Bind the user's address to the user's session.
   *
   * @param session The user's HTTP session, obtainable from
   *        request.getSession(true);
   * @param address The user's address.
   */
  public void setLoggedInAddress(HttpSession session, String address) {
    Preconditions.checkNotNull(session, "Session is null");
    session.setAttribute(ADDRESS_FIELD, address);
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
      session.removeAttribute(ADDRESS_FIELD);
    }
  }
}
