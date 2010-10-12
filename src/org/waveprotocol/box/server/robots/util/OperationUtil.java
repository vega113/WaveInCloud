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

package org.waveprotocol.box.server.robots.util;

import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.common.VersionedWaveletDelta;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.OperationResults;
import org.waveprotocol.box.server.robots.OperationServiceRegistry;
import org.waveprotocol.box.server.robots.RobotWaveletData;
import org.waveprotocol.box.server.robots.operations.OperationService;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;
import java.util.Map.Entry;

/**
 * {@link OperationRequest} utility methods.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class OperationUtil {

  private static final Log LOG = Log.get(OperationUtil.class);

  private OperationUtil() {
  }

  /**
   * Attempts to get a parameter, throws an exception if not found.
   *
   * @param <T> type of class to cast to.
   * @param operation operation to extract property from.
   * @param property the key of the parameter.
   * @return specialized object after being serialized.
   *
   * @throws InvalidRequestException if the property is not found.
   */
  @SuppressWarnings("unchecked")
  public static <T> T getRequiredParameter(OperationRequest operation, ParamsProperty property)
      throws InvalidRequestException {
    Object parameter = operation.getParameter(property);
    Class<T> clazz = (Class<T>) property.clazz();
    if (parameter == null || !clazz.isInstance(parameter)) {
      throw new InvalidRequestException("property " + property + " not found", operation);
    }
    return clazz.cast(parameter);
  }

  /**
   * Attempts to get a parameter, returns the {@code null} if not found.
   *
   * @param <T> type of class to cast to.
   * @param operation operation to extract property from.
   * @param key the key of the parameter.
   * @return specialized object after being serialized, or {@code null} if the
   *         parameter could not be found.
   */
  public static <T> T getOptionalParameter(OperationRequest operation, ParamsProperty key) {
    return OperationUtil.<T> getOptionalParameter(operation, key, null);
  }

  /**
   * Attempts to get a parameter, returns the default if not found.
   *
   * @param <T> type of class to cast to.
   * @param operation operation to extract property from.
   * @param property the key of the parameter.
   * @param defaultValue default value to return if parameter could not be
   *        found.
   * @return specialized object after being serialized, or the default value if
   *         the parameter could not be found.
   */
  @SuppressWarnings("unchecked")
  public static <T> T getOptionalParameter(
      OperationRequest operation, ParamsProperty property, T defaultValue) {
    Object parameter = operation.getParameter(property);
    Class<T> clazz = (Class<T>) property.clazz();
    if (parameter != null && clazz.isInstance(parameter)) {
      return clazz.cast(parameter);
    }
    return defaultValue;
  }

  /**
   * Determines the protocol version of a given operation bundle by inspecting
   * the first operation in the bundle. If it is a {@code robot.notify}
   * operation, and contains {@code protocolVersion} parameter, then this method
   * will return the value of that parameter. Otherwise, this method will return
   * the default version.
   *
   * @param operations the {@link OperationRequest}s to inspect.
   * @return the wire protocol version of the given operation bundle.
   */
  public static ProtocolVersion getProtocolVersion(List<OperationRequest> operations) {
    if (operations.size() == 0) {
      return ProtocolVersion.DEFAULT;
    }

    OperationRequest firstOperation = operations.get(0);
    if (firstOperation.getMethod().equals(OperationType.ROBOT_NOTIFY.method())) {
      String versionString = (String) firstOperation.getParameter(ParamsProperty.PROTOCOL_VERSION);
      if (versionString != null) {
        return ProtocolVersion.fromVersionString(versionString);
      }
    }
    return ProtocolVersion.DEFAULT;
  }

  /**
   * @return the type of operation present in the request
   */
  public static OperationType getOperationType(OperationRequest operation) {
    String methodName = operation.getMethod();

    // TODO(ljvderijk): This might be removed after the deserialization is fixed
    if (methodName.startsWith("wave.")) {
      methodName = methodName.replaceFirst("^wave[.]", "");
    }
    return OperationType.fromMethodName(methodName);
  }

  /**
   * Executes an {@link OperationRequest}. If the operation throws an
   * {@link InvalidRequestException} this exception will be used to construct an
   * error response in the {@link OperationContext}.
   *
   * @param operation the operation to be executed.
   * @param operationRegistry the registry containing the operations that can be
   *        performed.
   * @param context the context in which the operation is to be executed.
   * @param author the author of the operation.
   */
  public static void executeOperation(OperationRequest operation,
      OperationServiceRegistry operationRegistry, OperationContext context, ParticipantId author) {
    try {
      OperationService service =
          operationRegistry.getServiceFor(OperationUtil.getOperationType(operation));
      service.execute(operation, context, author);
    } catch (InvalidRequestException e) {
      LOG.warning("Operation " + operation + " failed to execute", e);
      context.constructErrorResponse(operation, e.getMessage());
    }
  }

  /**
   * Submits all deltas to the wavelet provider that are generated by the open
   * wavelets in the {@link OperationResults}.
   *
   * @param results the results of performing robot operations.
   * @param waveletProvider wavelet provider used to send the deltas to.
   * @param requestListener callback for deltas that are submitted to the
   *        wavelet provider.
   */
  public static void submitDeltas(OperationResults results, WaveletProvider waveletProvider,
      SubmitRequestListener requestListener) {
    for (Entry<WaveletName, RobotWaveletData> entry : results.getOpenWavelets().entrySet()) {
      WaveletName waveletName = entry.getKey();
      RobotWaveletData w = entry.getValue();
      for (VersionedWaveletDelta delta : w.getDeltas()) {
        ProtocolWaveletDelta protocolDelta = CoreWaveletOperationSerializer.serialize(delta.delta);
        waveletProvider.submitRequest(waveletName, protocolDelta, requestListener);
      }
    }
  }
}
