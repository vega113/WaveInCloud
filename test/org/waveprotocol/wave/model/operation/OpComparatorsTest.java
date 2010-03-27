// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.operation;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.bootstrap.BootstrapDocument;
import org.waveprotocol.wave.model.document.operation.BufferedDocInitialization;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.debug.RandomDocOpGenerator;
import org.waveprotocol.wave.model.document.operation.debug.RandomDocOpGenerator.Parameters;
import org.waveprotocol.wave.model.document.operation.debug.RandomDocOpGenerator.RandomProvider;
import org.waveprotocol.wave.model.document.operation.debug.RandomProviderImpl;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.operation.OpComparators.OpEquator;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;

public class OpComparatorsTest extends TestCase {


  // OpProtoConverterTest indirectly has plenty of positive tests, so this file
  // focuses on negative tests for now.

  public void testNullable() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    assertTrue(eq.equalNullable((WaveletOperation) null, null));
    assertTrue(eq.equalNullable((BufferedDocOp) null, null));
    assertFalse(eq.equalNullable(null, NoOp.INSTANCE));
    assertFalse(eq.equalNullable(null, new DocOpBuffer().finish()));
    assertFalse(eq.equalNullable(NoOp.INSTANCE, null));
    assertFalse(eq.equalNullable(new DocOpBuffer().finish(), null));

    try {
      eq.equal(null, NoOp.INSTANCE);
      fail();
    } catch (NullPointerException e) {
      // ok
    }

    try {
      eq.equal(NoOp.INSTANCE, null);
      fail();
    } catch (NullPointerException e) {
      // ok
    }

    try {
      eq.equal(new DocOpBuffer().finish(), null);
      fail();
    } catch (NullPointerException e) {
      // ok
    }

    try {
      eq.equal(null, new DocOpBuffer().finish());
      fail();
    } catch (NullPointerException e) {
      // ok
    }
  }

  public void testTypes() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    WaveletOperation a = NoOp.INSTANCE;
    WaveletOperation b = new AddParticipant(new ParticipantId(""));
    WaveletOperation c = new RemoveParticipant(new ParticipantId(""));
    WaveletOperation d = new WaveletDocumentOperation("", new DocOpBuffer().finish());

    assertTrue (eq.equal(a, a));
    assertFalse(eq.equal(a, b));
    assertFalse(eq.equal(a, c));
    assertFalse(eq.equal(a, d));

    assertFalse(eq.equal(b, a));
    assertTrue (eq.equal(b, b));
    assertFalse(eq.equal(b, c));
    assertFalse(eq.equal(b, d));

    assertFalse(eq.equal(c, a));
    assertFalse(eq.equal(c, b));
    assertTrue (eq.equal(c, c));
    assertFalse(eq.equal(c, d));

    assertFalse(eq.equal(d, a));
    assertFalse(eq.equal(d, b));
    assertFalse(eq.equal(d, c));
    assertTrue (eq.equal(d, d));
  }

  public void testAddParticipant() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    AddParticipant a1 = new AddParticipant(new ParticipantId("a"));
    AddParticipant a2 = new AddParticipant(new ParticipantId("a"));
    AddParticipant b1 = new AddParticipant(new ParticipantId("b"));
    AddParticipant b2 = new AddParticipant(new ParticipantId("b"));

    assertTrue(eq.equal(a1, a1));
    assertTrue(eq.equal(a1, a2));
    assertTrue(eq.equal(a2, a1));
    assertTrue(eq.equal(a2, a2));

    assertTrue(eq.equal(b1, b1));
    assertTrue(eq.equal(b1, b2));
    assertTrue(eq.equal(b2, b1));
    assertTrue(eq.equal(b2, b2));

    assertFalse(eq.equal(a1, b1));
    assertFalse(eq.equal(a1, b2));
    assertFalse(eq.equal(a2, b1));
    assertFalse(eq.equal(a2, b2));
  }

  public void testRemoveParticipant() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    RemoveParticipant a1 = new RemoveParticipant(new ParticipantId("a"));
    RemoveParticipant a2 = new RemoveParticipant(new ParticipantId("a"));
    RemoveParticipant b1 = new RemoveParticipant(new ParticipantId("b"));
    RemoveParticipant b2 = new RemoveParticipant(new ParticipantId("b"));

    assertTrue(eq.equal(a1, a1));
    assertTrue(eq.equal(a1, a2));
    assertTrue(eq.equal(a2, a1));
    assertTrue(eq.equal(a2, a2));

    assertTrue(eq.equal(b1, b1));
    assertTrue(eq.equal(b1, b2));
    assertTrue(eq.equal(b2, b1));
    assertTrue(eq.equal(b2, b2));

    assertFalse(eq.equal(a1, b1));
    assertFalse(eq.equal(a1, b2));
    assertFalse(eq.equal(a2, b1));
    assertFalse(eq.equal(a2, b2));
  }

  public void testWaveletDocumentOperationDocumentId() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    DocOpBuffer b = new DocOpBuffer();
    b.characters("a");
    BufferedDocOp d = b.finish();

    WaveletDocumentOperation a1 = new WaveletDocumentOperation("a", d);
    WaveletDocumentOperation a2 = new WaveletDocumentOperation("a", d);
    WaveletDocumentOperation b1 = new WaveletDocumentOperation("b", d);
    WaveletDocumentOperation b2 = new WaveletDocumentOperation("b", d);

    assertTrue(eq.equal(a1, a1));
    assertTrue(eq.equal(a1, a2));
    assertTrue(eq.equal(a2, a1));
    assertTrue(eq.equal(a2, a2));

    assertTrue(eq.equal(b1, b1));
    assertTrue(eq.equal(b1, b2));
    assertTrue(eq.equal(b2, b1));
    assertTrue(eq.equal(b2, b2));

    assertFalse(eq.equal(a1, b1));
    assertFalse(eq.equal(a1, b2));
    assertFalse(eq.equal(a2, b1));
    assertFalse(eq.equal(a2, b2));
  }

  public void testWaveletDocumentOperationDocOp() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    DocOpBuffer ba = new DocOpBuffer();
    ba.characters("a");
    BufferedDocOp da = ba.finish();
    DocOpBuffer bb = new DocOpBuffer();
    bb.deleteCharacters("a");
    BufferedDocOp db = bb.finish();

    WaveletDocumentOperation a1 = new WaveletDocumentOperation("a", da);
    WaveletDocumentOperation a2 = new WaveletDocumentOperation("a", da);
    WaveletDocumentOperation b1 = new WaveletDocumentOperation("a", db);
    WaveletDocumentOperation b2 = new WaveletDocumentOperation("a", db);

    assertTrue(eq.equal(a1, a1));
    assertTrue(eq.equal(a1, a2));
    assertTrue(eq.equal(a2, a1));
    assertTrue(eq.equal(a2, a2));

    assertTrue(eq.equal(b1, b1));
    assertTrue(eq.equal(b1, b2));
    assertTrue(eq.equal(b2, b1));
    assertTrue(eq.equal(b2, b2));

    assertFalse(eq.equal(a1, b1));
    assertFalse(eq.equal(a1, b2));
    assertFalse(eq.equal(a2, b1));
    assertFalse(eq.equal(a2, b2));
  }

  public void testDocOp() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    DocOpBuffer ba1 = new DocOpBuffer();
    ba1.characters("a");
    BufferedDocOp a1 = ba1.finish();

    DocOpBuffer ba2 = new DocOpBuffer();
    ba2.characters("a");
    BufferedDocOp a2 = ba2.finish();

    DocOpBuffer bb1 = new DocOpBuffer();
    bb1.deleteCharacters("a");
    BufferedDocOp b1 = bb1.finish();

    DocOpBuffer bb2 = new DocOpBuffer();
    bb2.deleteCharacters("a");
    BufferedDocOp b2 = bb1.finish();

    assertTrue(eq.equal(a1, a1));
    assertTrue(eq.equal(a1, a2));
    assertTrue(eq.equal(a2, a1));
    assertTrue(eq.equal(a2, a2));

    assertTrue(eq.equal(b1, b1));
    assertTrue(eq.equal(b1, b2));
    assertTrue(eq.equal(b2, b1));
    assertTrue(eq.equal(b2, b2));

    assertFalse(eq.equal(a1, b1));
    assertFalse(eq.equal(a1, b2));
    assertFalse(eq.equal(a2, b1));
    assertFalse(eq.equal(a2, b2));
  }

  /**
   * Tests a bugfix before which there was a possible ambiguity with annotation
   * keys containing spaces: Ending the single annotation 'x y' could not be
   * distinguished from ending the two annotations 'x' and 'y'. This case
   * verifies that the two cases are considered distinct.
   */
  public void testEqualHandlesSpacesInAnnotationKeys() {
    BufferedDocInitialization doc1 = DocOpUtil.buffer(new DocInitializationBuilder()
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .updateValues(
                "x", null, "1",
                "x y", null, "3",
                "y", null, "2").build())
        .characters("m")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x", "y").build())
        .characters("n")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x y").build())
        .build());

    BufferedDocInitialization doc2 = DocOpUtil.buffer(new DocInitializationBuilder()
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .updateValues(
                "x", null, "1",
                "x y", null, "3",
                "y", null, "2").build())
        .characters("m")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x y").build())
        .characters("n")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x", "y").build())
        .build());
    assertFalse("\ndoc1: " + doc1 + "\ndoc2: " + doc2,
        OpComparators.SYNTACTIC_IDENTITY.equal(doc1, doc2));
    assertFalse("\ndoc1: " + DocOpUtil.toXmlString(doc1) + "\ndoc2: " + DocOpUtil.toXmlString(doc2),
        OpComparators.equalDocuments(doc1, doc2));
  }

  /**
   * Tests that annotation keys can contain double quotes (") without causing
   * any ambiguity in equality checks.
   */
  public void testEqualHandlesQuotesInAnnotationKeys() {
    BufferedDocInitialization doc1 = DocOpUtil.buffer(new DocInitializationBuilder()
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .updateValues(
                "x", null, "1",
                "x\" \"y", null, "3",
                "y", null, "2").build())
        .characters("m")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x", "y").build())
        .characters("n")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x\" \"y").build())
        .build());

    BufferedDocInitialization doc2 = DocOpUtil.buffer(new DocInitializationBuilder()
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .updateValues(
                "x", null, "1",
                "x\" \"y", null, "3",
                "y", null, "2").build())
        .characters("m")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x\" \"y").build())
        .characters("n")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x", "y").build())
        .build());
    assertFalse("\ndoc1: " + doc1 + "\ndoc2: " + doc2,
        OpComparators.SYNTACTIC_IDENTITY.equal(doc1, doc2));
    assertFalse("\ndoc1: " + DocOpUtil.toXmlString(doc1) + "\ndoc2: " + DocOpUtil.toXmlString(doc2),
        OpComparators.equalDocuments(doc1, doc2));
  }

  public void testRandomDocOps() throws OperationException {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    Parameters p = new Parameters();
    for (int i = 0; i < 200; i++) {
      BootstrapDocument doc = new BootstrapDocument();
      for (int j = 0; j < 20; j++) {
        RandomProvider ra = RandomProviderImpl.ofSeed(i * 20 + j);
        RandomProvider rb = RandomProviderImpl.ofSeed(i * 20 + j + 1);
        BufferedDocOp a = DocOpUtil.buffer(RandomDocOpGenerator.generate(ra, p, doc));
        BufferedDocOp b = DocOpUtil.buffer(RandomDocOpGenerator.generate(rb, p, doc));
        doc.consume(a);
        assertTrue(eq.equal(a, a));
        // The combination of RandomProvider and RandomDocOpGenerator doesn't
        // really guarantee this property, but it happens to be true with the
        // random seeds that occur here.
        assertFalse(eq.equal(a, b));
      }
    }
  }

}
