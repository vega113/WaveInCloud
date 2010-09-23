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

import java.util.HashMap;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;

/**
 * A helper for getting the authentication configuration, or creating it if it
 * doesn't exist.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class ConfigurationProvider {
  public static final String CONTEXT_NAME = "Wave";

  private static Configuration makeDefaultConfiguration() {
    return new Configuration() {
      @Override
      public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        if (name.equals(CONTEXT_NAME)) {
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

  /**
   * Get the current JAAS configuration. The default configuration is provided
   * if JAAS has not been configured.
   */
  public static Configuration get() {
    Configuration configuration;
    try {
      configuration = Configuration.getConfiguration();
    } catch (SecurityException e) {
      // A SecurityException is thrown if the user hasn't configured a login
      // configuration.

      // If no other configuration is specified, create one programatically.
      configuration = makeDefaultConfiguration();
    }

    return configuration;
  }
}
