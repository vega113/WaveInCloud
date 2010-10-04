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

package org.waveprotocol.wave.examples.fedone.robots;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.wave.api.RobotSerializer;
import com.google.wave.api.data.converter.EventDataConverterModule;
import com.google.wave.api.robot.HttpRobotConnection;
import com.google.wave.api.robot.RobotConnection;

import net.oauth.OAuthServiceProvider;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.waveprotocol.wave.examples.fedone.robots.active.ActiveApiOperationServiceRegistry;
import org.waveprotocol.wave.examples.fedone.robots.dataapi.DataApiOperationServiceRegistry;
import org.waveprotocol.wave.examples.fedone.robots.passive.RobotConnector;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Robot API Module.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotApiModule extends AbstractModule {

  private static final int NUMBER_OF_THREADS = 10;

  @Override
  protected void configure() {
    install(new EventDataConverterModule());
    install(new RobotSerializerModule());
  }

  @Provides
  @Inject
  @Singleton
  protected RobotConnector provideRobotConnector(
      RobotConnection connection, RobotSerializer serializer) {
    return new RobotConnector(connection, serializer);
  }

  @Provides
  @Singleton
  protected RobotConnection provideRobotConnection() {
    HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());

    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("RobotConnection").build();
    return new HttpRobotConnection(
        httpClient, Executors.newFixedThreadPool(NUMBER_OF_THREADS, threadFactory));
  }

  @Provides
  @Singleton
  @Named("GatewayExecutor")
  protected Executor provideGatewayExecutor() {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("PassiveRobotRunner").build();
    return Executors.newFixedThreadPool(NUMBER_OF_THREADS, threadFactory);
  }

  @Provides
  @Singleton
  @Named("ActiveApiRegistry")
  protected OperationServiceRegistry provideActiveApiRegistry() {
    return new ActiveApiOperationServiceRegistry();
  }

  @Provides
  @Singleton
  @Named("DataApiRegistry")
  protected OperationServiceRegistry provideDataApiRegistry() {
    return new DataApiOperationServiceRegistry();
  }

  @Provides
  @Singleton
  protected OAuthValidator provideOAuthValidator() {
    // TODO(ljvderijk): This isn't an industrial strength validator, it grows
    // over time. It should be replaced or cleaned out on a regular interval.
    return new SimpleOAuthValidator();
  }

  @Provides
  @Singleton
  protected OAuthServiceProvider provideOAuthServiceProvider() {
    // TODO (ljvderijk): For the Data api we need the host name here.
    // Three urls, first is for the unauthorized request token, second is to
    // authorize the request token, third is to exchange the authorized request
    // token with an access token.
    return new OAuthServiceProvider("http://%s/robot/dataapi/request",
        "http://%s/robot/dataapi/authorize", "http://%s/robot/dataapi/exchange");
  }
}
