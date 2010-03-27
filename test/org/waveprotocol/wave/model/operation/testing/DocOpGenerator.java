// Copyright 2009 Google Inc. All Rights Reserved.


package org.waveprotocol.wave.model.operation.testing;

import org.waveprotocol.wave.model.document.bootstrap.BootstrapDocument;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.debug.RandomDocOpGenerator;
import org.waveprotocol.wave.model.document.operation.debug.RandomProviderImpl;
import org.waveprotocol.wave.model.document.operation.debug.RandomDocOpGenerator.RandomProvider;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;

import java.util.Random;

public class DocOpGenerator implements RandomOpGenerator<BootstrapDocument, BufferedDocOp> {

  @Override
  public BufferedDocOp randomOperation(BootstrapDocument state, Random random) {
    RandomProvider randomProvider = new RandomProviderImpl(random);
    EvaluatingDocOpCursor<BufferedDocOp> builder = new DocOpBuffer();
    RandomDocOpGenerator.generate(randomProvider, new RandomDocOpGenerator.Parameters(),
        state).apply(builder);
    return builder.finish();
  }

}
