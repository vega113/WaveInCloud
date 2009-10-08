/**
 * Copyright 2009 Google Inc.
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

package org.waveprotocol.wave.examples.fedone.crypto;

import junit.framework.TestCase;

import org.waveprotocol.wave.crypto.DefaultTrustRootsProvider;
import org.waveprotocol.wave.crypto.TrustRootsProvider;

public class DefaultTrustRootsProviderTest extends TestCase {

  public void testGetCertificates() throws Exception {

    // It's hard to predict what CAs will be trusted on any given platform,
    // but we'll test here that there are at least some CAs defined by default.
    // Otherwise, the prototype server won't work out of the box.
    TrustRootsProvider provider = new DefaultTrustRootsProvider();
    assertTrue(provider.getTrustRoots().size() > 0);
  }
}
