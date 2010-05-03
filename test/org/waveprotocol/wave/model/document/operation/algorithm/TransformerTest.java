// Copyright 2010 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.operation.algorithm;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.OperationException;

/**
*
 */
public class TransformerTest extends TestCase {

  public void testClientOpLongerThanServerOp() throws OperationException {
    try {
      Transformer.transform(new DocOpBuilder().retain(1).build(), new DocOpBuilder().build());
      fail();
    } catch (OperationException e) {
      // ok
    }
  }

}
