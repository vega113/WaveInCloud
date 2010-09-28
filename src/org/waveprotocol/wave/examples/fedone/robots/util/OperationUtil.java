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

package org.waveprotocol.wave.examples.fedone.robots.util;

import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.ProtocolVersion;

import java.util.List;

/**
 * {@link OperationRequest} utility methods.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class OperationUtil {

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
    // TODO(ljvderijk): Add tests
    String methodName = operation.getMethod();

    // TODO(ljvderijk): This might be removed after the deserialization is fixed
    if (methodName.startsWith("wave.")) {
      methodName = methodName.replaceFirst("^wave[.]", "");
    }
    return OperationType.fromMethodName(methodName);
  }
}
