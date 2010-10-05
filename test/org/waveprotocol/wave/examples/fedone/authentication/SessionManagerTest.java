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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.account.HumanAccountData;
import org.waveprotocol.wave.examples.fedone.account.HumanAccountDataImpl;
import org.waveprotocol.wave.examples.fedone.persistence.AccountStore;
import org.waveprotocol.wave.examples.fedone.persistence.memory.MemoryStore;
import org.waveprotocol.wave.model.wave.ParticipantId;

import javax.servlet.http.HttpSession;

/**
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class SessionManagerTest extends TestCase {
  private SessionManager sessionManager;
  private HumanAccountData account;

  @Override
  protected void setUp() throws Exception {
    AccountStore store = new MemoryStore();
    account = new HumanAccountDataImpl(ParticipantId.ofUnsafe("tubes@example.com"));
    store.putAccount(account);
    sessionManager = new SessionManager(store);
  }

  public void testSessionFetchesAddress() {
    HttpSession session = mock(HttpSession.class);
    ParticipantId id = ParticipantId.ofUnsafe("tubes@example.com");
    when(session.getAttribute("user")).thenReturn(id);

    assertEquals(id, sessionManager.getLoggedInUser(session));
    assertSame(account, sessionManager.getLoggedInAccount(session));
  }

  public void testUnknownUserReturnsNull() {
    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute("user")).thenReturn(ParticipantId.ofUnsafe("missing@example.com"));

    assertNull(sessionManager.getLoggedInAccount(session));
  }

  public void testNullSessionReturnsNull() {
    assertNull(sessionManager.getLoggedInUser(null));
    assertNull(sessionManager.getLoggedInAccount(null));
  }

  public void testGetLoginUrlWithNoArgument() {
    assertEquals(SessionManager.AUTH_URL, sessionManager.getLoginUrl(null));
  }

  public void testGetLoginUrlWithSimpleRedirect() {
    assertEquals(SessionManager.AUTH_URL + "?r=/some/other/url",
        sessionManager.getLoginUrl("/some/other/url"));
  }

  public void testGetLoginUrlEncodesQueryParameters() {
    String url = "/abc123?nested=query&string";
    String encoded_url = "/abc123?nested%3Dquery%26string";
    assertEquals(SessionManager.AUTH_URL + "?r=" + encoded_url,
        sessionManager.getLoginUrl(url));
  }
}