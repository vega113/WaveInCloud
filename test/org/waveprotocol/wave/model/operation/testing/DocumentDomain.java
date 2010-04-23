// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.operation.testing;

import org.waveprotocol.wave.model.document.bootstrap.BootstrapDocument;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.document.operation.algorithm.Transformer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.operation.Domain;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;

public class DocumentDomain implements Domain<BootstrapDocument, BufferedDocOp> {

  @Override
  public BootstrapDocument initialState() {
    return new BootstrapDocument();
  }

  @Override
  public void apply(BufferedDocOp op, BootstrapDocument state) throws OperationException {
    state.consume(op);
  }

  @Override
  public BufferedDocOp compose(BufferedDocOp f, BufferedDocOp g) throws OperationException {
    return Composer.compose(g, f);
  }

  @Override
  public OperationPair<BufferedDocOp> transform(BufferedDocOp clientOp, BufferedDocOp serverOp)
      throws TransformException {
    return Transformer.transform(clientOp, serverOp);
  }

  @Override
  public BufferedDocOp invert(BufferedDocOp operation) {
    EvaluatingDocOpCursor<BufferedDocOp> inverter =
        new DocOpInverter<BufferedDocOp>(new DocOpBuffer());
    operation.apply(inverter);
    return inverter.finish();
  }

  @Override
  public BufferedDocOp asOperation(BootstrapDocument state) {
    EvaluatingDocOpCursor<BufferedDocOp> builder = new DocOpBuffer();
    state.asOperation().apply(builder);
    return builder.finish();
  }

  @Override
  public boolean equivalent(BootstrapDocument state1, BootstrapDocument state2) {
    return state1.toString().equals(state2.toString());
  }

}
