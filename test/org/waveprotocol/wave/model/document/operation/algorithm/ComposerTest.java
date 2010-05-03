// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.operation.algorithm;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.OperationException;

/**
 * @author ohler@google.com (Christian Ohler)
 */
public class ComposerTest extends TestCase {

  public void testDocumentLengthMismatch() {
    try {
      Composer.compose(new DocOpBuilder().build(), new DocOpBuilder().retain(1).build());
      fail();
    } catch (OperationException e) {
      // ok
    }
    try {
      Composer.compose(new DocOpBuilder().retain(1).build(), new DocOpBuilder().build());
      fail();
    } catch (OperationException e) {
      // ok
    }
  }

}
