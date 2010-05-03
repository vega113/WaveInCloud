// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.operation.wave;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Tests for the {@code WaveletOperation} containers.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public class WaveletOperationTest extends TestCase {

  private static final String DOMAIN = "example.com";
  private static final WaveId WAVE_ID = new WaveId(DOMAIN, "hello");
  private static final WaveletId WAVELET_ID = new WaveletId(DOMAIN, "world");
  private static final ParticipantId PARTICIPANT_ID = new ParticipantId("test@" + DOMAIN);
  private static final String DOC_ID = "doc";
  private static final String TEXT = "hi";

  private final WaveletOperation addOp;
  private final WaveletOperation removeOp;
  private final WaveletOperation noOp;
  private final WaveletOperation docOpCharacters;
  private final WaveletOperation docOpDeleteCharacters;
  private final WaveletOperation docOpRetainAndCharacters;
  private final WaveletOperation docOpRetainAndDeleteCharacters;
  private final WaveletDocumentOperation docOpComplex;

  private WaveletData wavelet;
  private WaveletData originalWavelet;

  public WaveletOperationTest() {
    DocOpBuffer docOpBuilder;

    addOp = new AddParticipant(PARTICIPANT_ID);
    removeOp = new RemoveParticipant(PARTICIPANT_ID);
    noOp = NoOp.INSTANCE;

    docOpBuilder = new DocOpBuffer();
    docOpBuilder.characters(TEXT);
    docOpCharacters = new WaveletDocumentOperation(DOC_ID, docOpBuilder.finish());

    docOpBuilder = new DocOpBuffer();
    docOpBuilder.deleteCharacters(TEXT);
    docOpDeleteCharacters = new WaveletDocumentOperation(DOC_ID, docOpBuilder.finish());

    docOpBuilder = new DocOpBuffer();
    docOpBuilder.retain(TEXT.length());
    docOpBuilder.characters(TEXT);
    docOpRetainAndCharacters = new WaveletDocumentOperation(DOC_ID, docOpBuilder.finish());

    docOpBuilder = new DocOpBuffer();
    docOpBuilder.retain(TEXT.length());
    docOpBuilder.deleteCharacters(TEXT);
    docOpRetainAndDeleteCharacters = new WaveletDocumentOperation(DOC_ID, docOpBuilder.finish());

    docOpBuilder = new DocOpBuffer();
    docOpBuilder.elementStart("name1",
        new AttributesImpl(ImmutableMap.of("key1", "val1", "key2", "val2")));
    docOpBuilder.characters(TEXT);
    docOpBuilder.elementStart("name2",
        new AttributesImpl(ImmutableMap.of("key3", "val3", "key4", "val4")));
    docOpBuilder.characters(TEXT + TEXT);
    docOpBuilder.elementEnd();
    docOpBuilder.characters(TEXT + TEXT + TEXT);
    docOpBuilder.elementEnd();
    docOpComplex = new WaveletDocumentOperation(DOC_ID, docOpBuilder.finish());
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    wavelet = new WaveletDataImpl(WAVE_ID, WAVELET_ID);
    originalWavelet = new WaveletDataImpl(WAVE_ID, WAVELET_ID);
  }

  // Tests

  public void testTypeSanity() {
    assertTrue(noOp.getInverse() instanceof NoOp);
    assertTrue(addOp.getInverse() instanceof RemoveParticipant);
    assertTrue(removeOp.getInverse() instanceof AddParticipant);
    assertTrue(docOpCharacters.getInverse() instanceof WaveletDocumentOperation);
    assertTrue(docOpDeleteCharacters.getInverse() instanceof WaveletDocumentOperation);
    assertTrue(docOpRetainAndCharacters.getInverse() instanceof WaveletDocumentOperation);
    assertTrue(docOpRetainAndDeleteCharacters.getInverse() instanceof WaveletDocumentOperation);
  }

  public void testConcreteInverse() {
    assertOpEquals(addOp.getInverse(), removeOp);
    assertOpEquals(removeOp.getInverse(), addOp);
    assertOpEquals(docOpCharacters.getInverse(), docOpDeleteCharacters);
    assertOpEquals(docOpDeleteCharacters.getInverse(), docOpCharacters);
    assertOpEquals(docOpRetainAndCharacters.getInverse(), docOpRetainAndDeleteCharacters);
    assertOpEquals(docOpRetainAndDeleteCharacters.getInverse(), docOpRetainAndCharacters);
  }

  public void testAddParticipant() throws OperationException {
    assertOpInvertible(addOp);
    addOp.apply(wavelet);
    assertListEquals(wavelet.getParticipants(), ImmutableList.of(PARTICIPANT_ID));
  }

  public void testRemoveParticipant() throws OperationException {
    wavelet.addParticipant(PARTICIPANT_ID);
    originalWavelet.addParticipant(PARTICIPANT_ID);
    assertOpInvertible(removeOp);
  }

  public void testNoOp() throws OperationException {
    assertOpInvertible(noOp);
  }

  public void testCharacters() throws OperationException {
    assertOpInvertible(docOpCharacters);
  }

  public void testDeleteCharacters() throws OperationException {
    assertOpsInvertible(ImmutableList.of(docOpCharacters, docOpDeleteCharacters));
    docOpCharacters.apply(wavelet);
    docOpCharacters.apply(originalWavelet);
    assertOpInvertible(docOpDeleteCharacters);
  }

  public void testRetainAndCharacters() throws OperationException {
    assertOpsInvertible(ImmutableList.of(docOpCharacters, docOpRetainAndCharacters));
  }

  public void testRetainAndDeleteCharacters() throws OperationException {
    assertOpsInvertible(ImmutableList.of(
        docOpCharacters, docOpRetainAndCharacters, docOpRetainAndDeleteCharacters));
    docOpCharacters.apply(wavelet);
    docOpCharacters.apply(originalWavelet);
    assertOpsInvertible(ImmutableList.of(docOpRetainAndCharacters, docOpRetainAndDeleteCharacters));
  }

  public void testComplex() throws OperationException {
    assertOpInvertible(docOpComplex);
  }

  // Help

  private void assertOpInvertible(WaveletOperation op) throws OperationException {
    assertOpsInvertible(ImmutableList.of(op));
  }

  private void assertOpsInvertible(List<WaveletOperation> ops) throws OperationException {
    assertOpListEquals(ops, getInverse(getInverse(ops)));
    for (WaveletOperation op : ops) {
      op.apply(wavelet);
    }
    for (WaveletOperation op : getInverse(ops)) {
      op.apply(wavelet);
    }
    assertWaveletEquals(wavelet, originalWavelet);
  }

  private List<WaveletOperation> getInverse(List<WaveletOperation> ops) {
    List<WaveletOperation> inverse = Lists.newArrayList();
    for (WaveletOperation op : ops) {
      inverse.add(op.getInverse());
    }
    Collections.reverse(inverse);
    return inverse;
  }

  private void assertWaveletEquals(WaveletData w1, WaveletData w2) {
    assertEquals(w1.getWaveletName(), w2.getWaveletName());
    assertListEquals(w1.getParticipants(), w2.getParticipants());

    Map<String, BufferedDocOp> docs1 = w1.getDocuments();
    Map<String, BufferedDocOp> docs2 = w2.getDocuments();

    List<String> docKeys1 = Lists.newArrayList(docs1.keySet());
    Collections.sort(docKeys1);
    List<String> docKeys2 = Lists.newArrayList(docs2.keySet());
    Collections.sort(docKeys2);
    assertListEquals(docKeys1, docKeys2);

    for (String key : docKeys1) {
      assertOpEquals(docs1.get(key), docs2.get(key));
    }
  }

  private void assertOpListEquals(List<WaveletOperation> l1, List<WaveletOperation> l2) {
    assertEquals(l1.size(), l2.size());
    for (int i = 0; i < l1.size(); i++) {
      assertOpEquals(l1.get(i), l2.get(i));
    }
  }

  private void assertOpEquals(WaveletOperation op1, WaveletOperation op2) {
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(op1, op2));
  }

  private void assertOpEquals(BufferedDocOp op1, BufferedDocOp op2) {
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(op1, op2));
  }

  private void assertListEquals(List<?> l1, List<?> l2) {
    assertEquals(l1.size(), l2.size());
    for (int i = 0; i < l1.size(); i++) {
      assertEquals(l1.get(i), l2.get(i));
    }
  }
}
