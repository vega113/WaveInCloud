package org.waveprotocol.wave.model.document.operation.impl;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.BufferedDocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.AnnotationBoundary;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.Characters;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.DocInitializationComponent;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.DocOpComponent;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.ElementEnd;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.ElementStart;

import java.util.ArrayList;

/**
 * A builder for {@link BufferedDocInitialization}s.
 * 
 * Use {@link DocInitializationBuffer} instead if you need an implementation of
 * the interface
 * {@link org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor}.
 */
public class DocInitializationBuilder {
  private static final DocInitializationComponent[] EMPTY_ARRAY =
      new DocInitializationComponent[0];

  private final ArrayList<DocOpComponent> accu = new ArrayList<DocOpComponent>();

  /**
   * Constructs an operation from this builder's state.
   * 
   * Behaviour is undefined if this builder is used after calling this method.
   */
  public final BufferedDocInitialization build() {
    // TODO: This should not need a call to asInitialization().
    return DocOpUtil.asInitialization(BufferedDocOpImpl.create(accu.toArray(EMPTY_ARRAY)));
  }

  /** @see #build() */
  // This is dangerous; we currently use it for ill-formedness-detection
  // tests, and may use it for efficiency in other places in the future.
  public final BufferedDocInitialization buildUnchecked() {
    // TODO: This should not need a call to asInitialization().
    return DocOpUtil.asInitialization(BufferedDocOpImpl.createUnchecked(accu.toArray(EMPTY_ARRAY)));
  }

  public final DocInitializationBuilder annotationBoundary(AnnotationBoundaryMap map) {
    accu.add(new AnnotationBoundary(map));
    return this;
  }
  public final DocInitializationBuilder characters(String s) {
    accu.add(new Characters(s));
    return this;
  }
  public final DocInitializationBuilder elementEnd() {
    accu.add(ElementEnd.INSTANCE);
    return this;
  }
  public final DocInitializationBuilder elementStart(String type, Attributes attrs) {
    accu.add(new ElementStart(type, attrs));
    return this;
  }
}
