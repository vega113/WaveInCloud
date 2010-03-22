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

package org.waveprotocol.wave.federation.xmpp;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.waveprotocol.wave.federation.FederationErrors;

import junit.framework.TestCase;

/**
 * Performs naïve tests over RemoteDisco. Integration testing is performed in
 * {@link XmppDiscoTest}.
 *
 * @author thorogood@google.com (Sam Thorogood)
 */
public class RemoteDiscoTest extends TestCase {

  private final static String REMOTE_DOMAIN = "acmewave.com";
  private final static String REMOTE_JID = "wave.acmewave.com";

  @SuppressWarnings("unchecked")
  private SuccessFailCallback<String, String> mockDiscoCallback() {
    return mock(SuccessFailCallback.class);
  }

  /**
   * Test a RemoteDisco created with a forced success case.
   */
  public void testForcedSuccess() {
    RemoteDisco remoteDisco = new RemoteDisco(REMOTE_DOMAIN, REMOTE_JID, null);

    SuccessFailCallback<String, String> callback = mockDiscoCallback();
    remoteDisco.discoverRemoteJID(callback);
    verify(callback).onSuccess(eq(REMOTE_JID));
    verify(callback, never()).onFailure(anyString());
  }

  /**
   * Test a RemoteDisco created with a forced failure case.
   */
  public void testForcedFailure() {
    RemoteDisco remoteDisco = new RemoteDisco(REMOTE_DOMAIN, null,
        FederationErrors.badRequest("irrelevant"));

    SuccessFailCallback<String, String> callback = mockDiscoCallback();
    remoteDisco.discoverRemoteJID(callback);
    verify(callback, never()).onSuccess(anyString());
    verify(callback).onFailure(anyString());
  }

}
