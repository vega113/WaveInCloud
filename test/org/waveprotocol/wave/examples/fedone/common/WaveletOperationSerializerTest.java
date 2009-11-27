/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.wave.examples.fedone.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientUtils;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;

import java.util.Arrays;
import java.util.List;

/**
 * Tests {@link WaveletOperationSerializer}.
 *
 *
 */
public class WaveletOperationSerializerTest extends TestCase {

  private static void assertDeepEquals(WaveletOperation a, WaveletOperation b) {
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(a, b));
  }

  private static void assertDeepEquals(WaveletDelta a, WaveletDelta b) {
    assertEquals(a.getAuthor(), b.getAuthor());
    assertEquals(a.getOperations().size(), b.getOperations().size());
    int n = a.getOperations().size();
    for (int i = 0; i < n; i++) {
      assertDeepEquals(a.getOperations().get(i), b.getOperations().get(i));
    }
  }

  /**
   * Assert that an operation is unchanged when serialised then deserialised.
   *
   * @param op operation to check
   */
  private static void assertReversible(WaveletOperation op) {
    // Test both (de)serialising a single operation...
    assertDeepEquals(op, WaveletOperationSerializer.deserialize(
        WaveletOperationSerializer.serialize(op)));

    List<WaveletOperation> ops = ImmutableList.of(op, op, op);
    ParticipantId author = new ParticipantId("kalman@google.com");
    HashedVersion hashedVersion = HashedVersion.UNSIGNED_VERSION_0;
    WaveletDelta delta = new WaveletDelta(author, ops);
    ProtocolWaveletDelta serialized = WaveletOperationSerializer.serialize(delta, hashedVersion);
    Pair<WaveletDelta, HashedVersion> deserialized = WaveletOperationSerializer.deserialize(
        serialized);
    assertEquals(hashedVersion.getVersion(), serialized.getHashedVersion().getVersion());
    assertTrue(Arrays.equals(hashedVersion.getHistoryHash(),
        serialized.getHashedVersion().getHistoryHash().toByteArray()));
    assertDeepEquals(delta, deserialized.first);
  }

  public void testNoOp() {
    assertReversible(new NoOp());
  }

  public void testAddParticipant() {
    assertReversible(new AddParticipant(new ParticipantId("kalman@google.com")));
  }

  public void testRemoveParticipant() {
    assertReversible(new RemoveParticipant(new ParticipantId("kalman@google.com")));
  }

  public void testEmptyDocumentMutation() {
    assertReversible(new WaveletDocumentOperation("empty", ClientUtils.createEmptyDocument()));
  }

  public void testSingleCharacters() {
    DocOpBuilder m = new DocOpBuilder();

    m.characters("hello");

    assertReversible(new WaveletDocumentOperation("single", m.build()));
  }

  public void testManyCharacters() {
    DocOpBuilder m = new DocOpBuilder();

    m.characters("hello");
    m.characters("world");
    m.characters("foo");
    m.characters("bar");

    assertReversible(new WaveletDocumentOperation("many", m.build()));
  }

  public void testRetain() {
    DocOpBuilder m = new DocOpBuilder();

    m.characters("hello");
    m.retain(5);
    m.characters("world");
    m.retain(10);
    m.characters("foo");
    m.retain(13);
    m.characters("bar");
    m.retain(16);

    assertReversible(new WaveletDocumentOperation("retain", m.build()));
  }

  public void testDeleteCharacters() {
    DocOpBuilder m = new DocOpBuilder();

    m.characters("hello");
    m.retain(1);
    m.deleteCharacters("ab");
    m.characters("world");
    m.retain(2);
    m.deleteCharacters("cd");

    assertReversible(new WaveletDocumentOperation("deleteCharacters", m.build()));
  }

  public void testElements() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes a = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    Attributes b = new AttributesImpl();
    Attributes c = new AttributesImpl(ImmutableMap.of("c1", "1", "c2", "2", "c3", "3"));

    m.elementStart("a", a);
    m.elementStart("b", b);
    m.elementStart("c", c);
    m.elementEnd();
    m.elementEnd();
    m.elementEnd();

    assertReversible(new WaveletDocumentOperation("elements", m.build()));
  }

  public void testCharactersAndElements() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes a = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    Attributes b = new AttributesImpl();
    Attributes c = new AttributesImpl(ImmutableMap.of("c1", "1", "c2", "2", "c3", "3"));

    m.elementStart("a", a);
    m.characters("hello");
    m.elementStart("b", b);
    m.characters("world");
    m.elementStart("c", c);
    m.elementEnd();
    m.characters("blah");
    m.elementEnd();
    m.elementEnd();

    assertReversible(new WaveletDocumentOperation("charactersAndElements", m.build()));
  }

  public void testDeleteElements() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes a = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    Attributes b = new AttributesImpl();
    Attributes c = new AttributesImpl(ImmutableMap.of("c1", "1", "c2", "2", "c3", "3"));

    m.deleteElementStart("a", a);
    m.deleteElementStart("b", b);
    m.deleteElementStart("c", c);
    m.deleteElementEnd();
    m.deleteElementEnd();
    m.deleteElementEnd();

    assertReversible(new WaveletDocumentOperation("deleteElements", m.build()));
  }

  public void testDeleteCharactersAndElements() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes a = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    Attributes b = new AttributesImpl();
    Attributes c = new AttributesImpl(ImmutableMap.of("c1", "1", "c2", "2", "c3", "3"));

    m.deleteElementStart("a", a);
    m.deleteCharacters("hello");
    m.deleteElementStart("b", b);
    m.deleteCharacters("world");
    m.deleteElementStart("c", c);
    m.deleteElementEnd();
    m.deleteCharacters("blah");
    m.deleteElementEnd();
    m.deleteElementEnd();

    assertReversible(new WaveletDocumentOperation("deleteCharactersAndElements", m.build()));
  }
  
  public void testAnnotationBoundary() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes a = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    AnnotationBoundaryMap mapA = new AnnotationBoundaryMapImpl(
        new String[]{},new String[]{"a"},new String[]{null},new String[]{"b"});
    AnnotationBoundaryMap mapB = new AnnotationBoundaryMapImpl(
        new String[]{},new String[]{"a"},new String[]{"b"},new String[]{null});
    AnnotationBoundaryMap mapC = new AnnotationBoundaryMapImpl(
        new String[]{"a"},new String[]{},new String[]{},new String[]{});
    m.elementStart("a", a);
    m.annotationBoundary(mapA);
    m.characters("test");
    m.annotationBoundary(mapB);
    m.characters("text");
    m.annotationBoundary(mapC);
    m.elementEnd();

    assertReversible(new WaveletDocumentOperation("annotationBoundary", m.build()));
  }
  
  public void testEmptyAnnotationBoundary() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes a = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    m.elementStart("a", a);
    m.annotationBoundary(AnnotationBoundaryMapImpl.EMPTY_MAP);
    m.characters("text");
    m.annotationBoundary(AnnotationBoundaryMapImpl.EMPTY_MAP);
    m.elementEnd();

    assertReversible(new WaveletDocumentOperation("emptyAnnotationBoundary", m.build()));
  }
  
  public void testReplaceAttributes() {
    DocOpBuilder m = new DocOpBuilder();

    Attributes oldA = new AttributesImpl(ImmutableMap.of("a1", "1", "a2", "2"));
    Attributes newA = new AttributesImpl(ImmutableMap.of("a1", "3", "a2", "4"));

    m.retain(4);
    m.replaceAttributes(oldA, newA);
    m.retain(4);

    assertReversible(new WaveletDocumentOperation("replaceAttributes", m.build()));
  }
  
  public void testEmptyReplaceAttributes() {
    DocOpBuilder m = new DocOpBuilder();

    m.retain(4);
    m.replaceAttributes(AttributesImpl.EMPTY_MAP, AttributesImpl.EMPTY_MAP);
    m.retain(4);

    assertReversible(new WaveletDocumentOperation("emptyReplaceAttributes", m.build()));
  }
  
  public void testUpdateAttributes() {
    DocOpBuilder m = new DocOpBuilder();

    AttributesUpdate u = new AttributesUpdateImpl(new String[]{"a", null, "2", "b", "1", null});
    
    m.retain(4);
    m.updateAttributes(u);
    m.retain(4);
    
    assertReversible(new WaveletDocumentOperation("updateAttributes", m.build()));
  }
  
  public void testEmptyUpdateAttributes() {
    DocOpBuilder m = new DocOpBuilder();

    m.retain(4);
    m.updateAttributes(AttributesUpdateImpl.EMPTY_MAP);
    m.retain(4);
    
    assertReversible(new WaveletDocumentOperation("emptyUpdateAttributes", m.build()));
  }
}
