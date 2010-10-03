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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
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
  @Named("DataApiRegistry")
  protected OperationServiceRegistry provideDataApiRegistry() {
    return new DataApiOperationServiceRegistry();
  }
}
