// Copyright 2011 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.client.wave;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;

/**
 * Tests for diff control in LazyContentDocument.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public class LazyContentDocumentTest extends TestCase {

  @Mock
  private Registries registries;
  private LazyContentDocument document;
  private SimpleDiffDoc state;

  //
  //
  // empty --dempty--> x --dx--> y --dy--> z
  //

  private final static DocInitialization EMPTY = new DocInitializationBuilder().build();
  private final static DocOp D_EMPTY = new DocOpBuilder().characters("a").build();
  private final static DocOp D_X = new DocOpBuilder().retain(1).characters("b").build();
  private final static DocOp D_Y = new DocOpBuilder().retain(2).characters("c").build();
  private final static DocInitialization X;
  private final static DocInitialization Y;

  static {
    try {
      X = Composer.compose(EMPTY, D_EMPTY);
      Y = Composer.compose(X, D_X);
    } catch (OperationException e) {
      // Throwing exceptions in initializers is generally bad; but, in this
      // case, the data that can affect the statements above is clearly fine.
      throw new OperationRuntimeException("error in setting up sample states", e);
    }
  }

  @Override
  protected void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private void startWithCompleteState() {
    state = SimpleDiffDoc.create(X, null);
    assert state.isCompleteState() : "invalid test assumptions";
    document = LazyContentDocument.create(registries, state);
  }

  private void startWithStateAndDiff() {
    state = SimpleDiffDoc.create(X, D_X);
    assert !state.isCompleteDiff() && !state.isCompleteState() : "invalid test assumptions";
    document = LazyContentDocument.create(registries, state);
  }

  public void testOpOnCompleteStateIsADiff() {
    startWithCompleteState();
    document.consume(D_X);
    assertTrue(!state.isCompleteState());
  }

  public void testOpOnPartialIsADiff() {
    startWithStateAndDiff();
    document.consume(D_Y);
    assertTrue(!state.isCompleteState());
  }

  public void testDiffSuppressionClearsDiffs() {
    startWithStateAndDiff();
    document.startDiffSuppression();
    assertTrue(state.isCompleteState());
  }

  public void testNoDiffsWhileSuppressed() {
    startWithStateAndDiff();
    document.startDiffSuppression();
    document.consume(D_Y);
    assertTrue(state.isCompleteState());
    document.stopDiffSuppression();
    assertTrue(state.isCompleteState());
  }

  public void testLeavingSuppresionScopeStopsSuppresion() {
    startWithCompleteState();
    document.startDiffSuppression();
    document.consume(D_X);
    document.stopDiffSuppression();
    document.consume(D_Y);
    assertFalse(state.isCompleteDiff());
    assertFalse(state.isCompleteState());
  }

  public void testDiffClearingWorksWhenEnabled() {
    startWithStateAndDiff();
    document.clearDiffs();
    assertTrue(state.isCompleteState());
    assertTrue(OpComparators.equalDocuments(state.asOperation(), Y));
  }

  public void testDiffClearingHasNoEffectWhileDisabled() {
    startWithStateAndDiff();
    document.disableDiffClearing();
    document.clearDiffs();
    document.enableDiffClearing();
    assertFalse(state.isCompleteDiff());
    assertFalse(state.isCompleteState());
    assertTrue(OpComparators.equalDocuments(state.asOperation(), Y));
  }

  public void testScopingRules() {
    startWithCompleteState();
    try {
      document.stopDiffSuppression();
      fail("Left suppression while not suppressed");
    } catch (IllegalStateException expected) {
    }
    try {
      document.enableDiffClearing();
      fail("Left retention while not suppressed");
    } catch (IllegalStateException expected) {
    }

    document.disableDiffClearing();
    try {
      document.disableDiffClearing();
      fail("Entered nested retention scope");
    } catch (IllegalStateException expected) {
    }
    try {
      document.startDiffSuppression();
      fail("Entered suppression while retained");
    } catch (IllegalStateException expected) {
    }
    try {
      document.startDiffSuppression();
      fail("Entered suppression while retained");
    } catch (IllegalStateException expected) {
    }
    document.enableDiffClearing();

    document.startDiffSuppression();

    try {
      document.startDiffSuppression();
      fail("Entered nested suppression scope");
    } catch (IllegalStateException expected) {
    }
    document.disableDiffClearing();
    document.enableDiffClearing();
    document.stopDiffSuppression();
  }
}
