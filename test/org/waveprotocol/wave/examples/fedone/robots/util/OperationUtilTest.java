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
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.OperationType;
import com.google.wave.api.ProtocolVersion;

import junit.framework.TestCase;

import java.util.Collections;

/**
 * Unit tests for {@link OperationUtil}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class OperationUtilTest extends TestCase {

  private static final String WAVELET_ID = "example.com!conv+root";
  private static final String WAVE_ID = "example.com!waveid";

  private OperationRequest operation;

  @Override
  protected void setUp() throws Exception {
    operation = new OperationRequest("wavelet.fetch", "op1", WAVE_ID, WAVELET_ID);
  }

  public void testGetRequiredParameter() throws Exception {
    String waveId = OperationUtil.getRequiredParameter(operation, ParamsProperty.WAVE_ID);
    assertEquals(WAVE_ID, waveId);
  }

  public void testGetRequiredParameterThrowsInvalidRequestException() throws Exception {
    try {
      OperationUtil.getRequiredParameter(operation, ParamsProperty.ANNOTATION);
      fail("Expected InvalidRequestException");
    } catch (InvalidRequestException e) {
      // expected
    }
  }

  public void testGetOptionalParameter() throws Exception {
    String waveId = OperationUtil.getOptionalParameter(operation, ParamsProperty.WAVE_ID);
    assertEquals(WAVE_ID, waveId);

    assertNull("Non existing properties should return null when optional",
        OperationUtil.getOptionalParameter(operation, ParamsProperty.ANNOTATION));

    String defaultValue = "b+1234";
    String blipId =
        OperationUtil.getOptionalParameter(operation, ParamsProperty.BLIP_ID, defaultValue);
    assertSame("Default value should be returned when object does not exist", defaultValue, blipId);
  }

  public void testGetProtocolVersion() throws Exception {
    ProtocolVersion protocolVersion =
        OperationUtil.getProtocolVersion(Collections.<OperationRequest> emptyList());
    assertEquals(
        "Empty list should return default version", ProtocolVersion.DEFAULT, protocolVersion);

    protocolVersion = OperationUtil.getProtocolVersion(Collections.singletonList(operation));
    assertEquals("Non notify op as first op should return default", ProtocolVersion.DEFAULT,
        protocolVersion);

    OperationRequest notifyOp = new OperationRequest(OperationType.ROBOT_NOTIFY.method(), "op1");
    protocolVersion = OperationUtil.getProtocolVersion(Collections.singletonList(notifyOp));
    assertEquals("Notify op as first op without version parameter should return default",
        ProtocolVersion.DEFAULT, protocolVersion);

    Parameter versionParameter =
        Parameter.of(ParamsProperty.PROTOCOL_VERSION, ProtocolVersion.V2_1.getVersionString());
    notifyOp = new OperationRequest(OperationType.ROBOT_NOTIFY.method(), "op1", versionParameter);
    protocolVersion = OperationUtil.getProtocolVersion(Collections.singletonList(notifyOp));
    assertEquals(
        "Notify op as first op should return its version", ProtocolVersion.V2_1, protocolVersion);
  }
}
