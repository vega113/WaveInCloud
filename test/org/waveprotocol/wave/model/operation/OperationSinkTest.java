// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.operation;


import junit.framework.TestCase;


/**
 * Tests for the utilities provided with the [Silent]OperationSink interfaces.
 *
 * @author anorth@google.com (Alex North)
 */
public class OperationSinkTest extends TestCase {
  /**
   * A simple mock data class which can expect an operation, optionally
   * throwing OperationException.
   */
  final class MyData {
    boolean expecting = false;
    boolean failNextOperation = false;

    void expectOperation(boolean fail) {
      expecting = true;
      failNextOperation = fail;
    }

    void operation() throws OperationException {
      assertTrue(expecting);
      expecting = false;
      if (failNextOperation) {
        throw new OperationException("Failed for testing");
      }
    }

    void verify() {
      assertFalse(expecting);
    }
  }

  final class MyOperation implements Operation<MyData> {
    public void apply(MyData target) throws OperationException {
      target.operation();
    }
  }

  private MyData data;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    data = new MyData();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    data.verify();
  }

  /**
   * Tests the operation sink executor applies ops.
   */
  public void testOperationSinkExecutorAppliesOps() throws OperationException {
    OperationSink<MyOperation> sink = OperationSink.Executor.build(data);
    data.expectOperation(false);
    sink.consume(new MyOperation());
  }

  /**
   * Tests the operation sink executor throws an exception if
   * the op fails.
   */
  public void testOperationSinkExecutorRethrowsException() {
    OperationSink<MyOperation> sink = OperationSink.Executor.build(data);
    data.expectOperation(true);
    try {
      sink.consume(new MyOperation());
      fail("Expected an operation exception");
    } catch (OperationException expected) {
    }
  }

  /**
   * Tests that the silent operation sink executor applies ops.
   */
  public void testSilentOperationSinkExecutorAppliesOps() {
    SilentOperationSink<MyOperation> sink = SilentOperationSink.Executor.build(data);
    data.expectOperation(false);
    sink.consume(new MyOperation());
  }

  /**
   * Tests that the silent operation sink executor rethrows an exception
   * as an operation runtime exception.
   */
  public void testSilentOperationSinkExecutorAdaptsException() {
    SilentOperationSink<MyOperation> sink = SilentOperationSink.Executor.build(data);
    data.expectOperation(true);
    try {
      sink.consume(new MyOperation());
      fail("Expected an operation runtime exception");
    } catch (OperationRuntimeException expected) {
    }
  }
}
