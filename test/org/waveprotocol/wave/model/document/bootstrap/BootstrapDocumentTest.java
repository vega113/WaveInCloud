// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.bootstrap;

import org.waveprotocol.wave.model.document.operation.AbstractModifiableDocumentTest;
import org.waveprotocol.wave.model.document.operation.DocInitialization;

public class BootstrapDocumentTest extends AbstractModifiableDocumentTest<BootstrapDocument> {

  @Override
  protected DocInitialization asInitialization(BootstrapDocument document) {
    return document.asOperation();
  }

  @Override
  protected BootstrapDocument createEmptyDocument() {
    return new BootstrapDocument();
  }


}
