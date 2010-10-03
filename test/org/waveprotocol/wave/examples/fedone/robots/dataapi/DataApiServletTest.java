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

package org.waveprotocol.wave.examples.fedone.robots.dataapi;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.RobotSerializer;
import com.google.wave.api.data.converter.EventDataConverterManager;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.robots.OperationContext;
import org.waveprotocol.wave.examples.fedone.robots.OperationServiceRegistry;
import org.waveprotocol.wave.examples.fedone.robots.operations.OperationService;
import org.waveprotocol.wave.examples.fedone.robots.util.ConversationUtil;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Unit tests for the {@link DataApiServlet}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class DataApiServletTest extends TestCase {

  private static final ParticipantId ALEX = ParticipantId.ofUnsafe("alex@example.com");

  private RobotSerializer robotSerializer;
  private EventDataConverterManager converterManager;
  private WaveletProvider waveletProvider;
  private OperationServiceRegistry operationRegistry;
  private DataApiServlet servlet;

  @Override
  protected void setUp() {
    robotSerializer = mock(RobotSerializer.class);
    converterManager = mock(EventDataConverterManager.class);
    waveletProvider = mock(WaveletProvider.class);
    operationRegistry = mock(OperationServiceRegistry.class);
    ConversationUtil conversationUtil = mock(ConversationUtil.class);

    servlet = new DataApiServlet(
        robotSerializer, converterManager, waveletProvider, operationRegistry, conversationUtil);
  }

  public void testDoPostExecutesAndWritesResponse() throws Exception {
    HttpServletRequest req = mock(HttpServletRequest.class);
    when(req.getHeader(DataApiServlet.USERNAME_HEADER)).thenReturn(ALEX.getAddress());
    when(req.getReader()).thenReturn(new BufferedReader(new StringReader("")));

    HttpServletResponse resp = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    when(resp.getWriter()).thenReturn(writer);

    String operationId = "op1";
    OperationRequest operation = new OperationRequest("wavelet.create", operationId);
    List<OperationRequest> operations = Collections.singletonList(operation);
    when(robotSerializer.deserializeOperations(anyString())).thenReturn(operations);
    String responseValue = "response value";
    when(robotSerializer.serialize(any(), any(Type.class), any(ProtocolVersion.class))).thenReturn(
        responseValue);

    OperationService service = mock(OperationService.class);
    when(operationRegistry.getServiceFor(any(OperationType.class))).thenReturn(service);

    servlet.doPost(req, resp);

    verify(operationRegistry).getServiceFor(any(OperationType.class));
    verify(service).execute(eq(operation), any(OperationContext.class), eq(ALEX));
    verify(resp).setStatus(HttpServletResponse.SC_OK);
    assertEquals("Response should have been written into the servlet", responseValue,
        stringWriter.toString());
  }
}
