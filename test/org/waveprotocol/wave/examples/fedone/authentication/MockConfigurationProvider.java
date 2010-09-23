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

package org.waveprotocol.wave.examples.fedone.authentication;

import java.util.HashMap;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;

/**
 * Provides a mock security configuration for tests.
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class MockConfigurationProvider {
  public static Configuration make() {
    return new Configuration() {
      @Override
      public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        if (name.equals(ConfigurationProvider.CONTEXT_NAME)) {
          AppConfigurationEntry entry =
              new AppConfigurationEntry(AccountStoreLoginModule.class.getName(),
                  LoginModuleControlFlag.REQUIRED, new HashMap<String, Object>());

          return new AppConfigurationEntry[] {entry};
        } else {
          return null;
        }
      }
    };
  }
}
