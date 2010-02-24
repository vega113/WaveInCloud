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

import org.waveprotocol.wave.federation.xmpp.XmppDisco;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Tiny MockDisco class that wraps XmppDisco.
 *
 * Use {@link #testInjectInDomainToJidMap} to configure custom immediate
 * responses, otherwise responses will be placed on a pending queue.
 *
 * @author thorogood@google.com (Sam Thorogood)
 */
public class MockDisco extends XmppDisco {

  MockDisco(String serverName) {
    super(serverName);
  }

  public static class PendingMockDisco {
    public final String remoteDomain;
    public final SuccessFailCallback<String, String> callback;

    private PendingMockDisco(String remoteDomain, SuccessFailCallback<String, String> callback) {
      this.remoteDomain = remoteDomain;
      this.callback = callback;
    }
  }

  public Queue<PendingMockDisco> pending = new LinkedList<PendingMockDisco>();

  @Override
  public void discoverRemoteJid(String remoteDomain, SuccessFailCallback<String, String> callback) {
    if (isDiscoRequestAvailable(remoteDomain)) {
      // Note: tiny race condition in case this is purged between above and
      // below, but since this is only used in tests, we can probably ignore it.
      super.discoverRemoteJid(remoteDomain, callback);
    } else {
      pending.add(new PendingMockDisco(remoteDomain, callback));
    }
  }

}
