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

package org.waveprotocol.box.server.robots.dataapi;

import com.google.wave.api.OperationType;

import org.waveprotocol.box.server.robots.AbstractOperationServiceRegistry;
import org.waveprotocol.box.server.robots.operations.AppendBlipService;
import org.waveprotocol.box.server.robots.operations.CreateWaveletService;
import org.waveprotocol.box.server.robots.operations.DoNothingService;
import org.waveprotocol.box.server.robots.operations.FetchWaveService;
import org.waveprotocol.box.server.robots.operations.OperationService;

/**
 * A registry of {@link OperationService}s for the data API.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public final class DataApiOperationServiceRegistry extends AbstractOperationServiceRegistry {

  // Suppressing warnings about operations that are deprecated but still used by
  // the default client libraries
  @SuppressWarnings("deprecation")
  public DataApiOperationServiceRegistry() {
    super();

    // Register all the OperationProviders
    register(OperationType.ROBOT_NOTIFY, DoNothingService.create());
    register(OperationType.ROBOT_NOTIFY_CAPABILITIES_HASH, DoNothingService.create());
    register(OperationType.WAVELET_APPEND_BLIP, AppendBlipService.create());
    register(OperationType.ROBOT_CREATE_WAVELET, CreateWaveletService.create());
    register(OperationType.ROBOT_FETCH_WAVE, FetchWaveService.create());
  }
}
