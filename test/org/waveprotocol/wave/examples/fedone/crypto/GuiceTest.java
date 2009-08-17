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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Named;

import junit.framework.TestCase;

public class GuiceTest extends TestCase {

  private static final String PATH =
      "test/org/waveprotocol/wave/examples/fedone/crypto/";

  public void testProvisioning() throws Exception {

    // make sure we can make a WaveSigner
    Injector injector = Guice.createInjector(new TestModule());
    injector.getInstance(WaveSigner.class);
  }

  private static class TestModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides @Named("certificate_domain")
    public String getDomain() {
      return "example.com";
    }

    @Provides @Named("certificate_private_key")
    public String getPrivateKeyFile() {
      return PATH + "test.key";
    }

    @Provides @Named("certificate_files")
    public String getCertFiles() {
      return PATH + "test.cert";
    }
  }
}
