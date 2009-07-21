// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.examples.fedone.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientUtils;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.operation.impl.BufferedDocOpImpl.DocOpBuilder;
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

  private static void assertDeepEquals(BufferedDocOp a, BufferedDocOp b) {
    assertEquals(a.size(), b.size());
    // TODO: Comparing the stringified operations is a hack.
    assertEquals(DocOpUtil.toConciseString(a), DocOpUtil.toConciseString(b));
  }

  private static void assertDeepEquals(WaveletDocumentOperation a, WaveletDocumentOperation b) {
    assertEquals(a.getDocumentId(), b.getDocumentId());
    assertDeepEquals(a.getOperation(), b.getOperation());
  }

  private static void assertDeepEquals(WaveletOperation a, WaveletOperation b) {
    if (a instanceof AddParticipant) {
      assertEquals(a, b);
    } else if (a instanceof RemoveParticipant) {
      assertEquals(a, b);
    } else if (a instanceof NoOp) {
      assertEquals(a, b);
    } else if (a instanceof WaveletDocumentOperation) {
      if (b instanceof WaveletDocumentOperation) {
        assertDeepEquals((WaveletDocumentOperation) a, (WaveletDocumentOperation) b);
      } else {
        assertEquals(a, b);
      }
    } else {
      assertEquals(a, b);
    }
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

    assertReversible(new WaveletDocumentOperation("single", m.finish()));
  }

  public void testManyCharacters() {
    DocOpBuilder m = new DocOpBuilder();

    m.characters("hello");
    m.characters("world");
    m.characters("foo");
    m.characters("bar");

    assertReversible(new WaveletDocumentOperation("many", m.finish()));
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

    assertReversible(new WaveletDocumentOperation("retain", m.finish()));
  }

  public void testDeleteCharacters() {
    DocOpBuilder m = new DocOpBuilder();

    m.characters("hello");
    m.retain(1);
    m.deleteCharacters("ab");
    m.characters("world");
    m.retain(2);
    m.deleteCharacters("cd");

    assertReversible(new WaveletDocumentOperation("deleteCharacters", m.finish()));
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

    assertReversible(new WaveletDocumentOperation("elements", m.finish()));
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

    assertReversible(new WaveletDocumentOperation("charactersAndElements", m.finish()));
  }
}
