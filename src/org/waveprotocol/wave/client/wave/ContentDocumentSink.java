/**
 * Copyright 2010 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.waveprotocol.wave.client.wave;

import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.concurrencycontrol.wave.CcDocument;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.util.MutableDocumentProxy;
import org.waveprotocol.wave.model.operation.SilentOperationSink;

/**
 * A document implementation that materializes a {@link ContentDocument} on
 * demand.
 *
 */
public final class ContentDocumentSink extends MutableDocumentProxy<Doc.N, Doc.E, Doc.T>
    implements CcDocument, Document {

  /** Event handlers. */
  private final Registries base;

  /** Lazily-loaded document implementation. Never cleared once set. */
  private ContentDocument document;

  /** Initial document state. Nulled after initialization. */
  private DocInitialization content;

  /** Sink to which local mutations are sent. Never cleared once set. */
  private SilentOperationSink<? super DocOp> outputSink;

  /**
   * Whether the MutableDocumentProxy's delegate has been set. Never cleared
   * once set.
   */
  private boolean delegateSet;

  ContentDocumentSink(Registries base, DocInitialization content) {
    this.base = base;
    this.content = content;
  }

  /**
   * Loads the real document implementation with a particular set of registries.
   */
  private void loadWith(Registries registries) {
    assert document == null : "already loaded";
    document = new ContentDocument(DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    if (outputSink != null) {
      document.setOutgoingSink(outputSink);
    }
    document.setRegistries(registries);
    if (content != null) {
      document.consume(content);
      content = null;
    }
  }

  /** @return the document implementation, materializing it if necessary. */
  public ContentDocument getDocument() {
    if (document == null) {
      // ContentDocument currently requires a base set of registries in order to
      // function. Line-container stuff in particular.
      loadWith(base);
    }
    return document;
  }

  /**
   * {@inheritDoc}
   *
   * The document is materialized if necessary.
   */
  @SuppressWarnings("unchecked") // Type conversion with flattened generics.
  @Override
  public Document getMutableDocument() {
    if (!delegateSet) {
      setDelegate((MutableDocument) getDocument().getMutableDoc());
      delegateSet = true;
    }
    return this;
  }

  @Override
  public void init(SilentOperationSink<? super DocOp> outputSink) {
    this.outputSink = outputSink;
  }

  @Override
  public DocInitialization asOperation() {
    return document != null ? getDocument().asOperation() : content;
  }

  @Override
  public void consume(DocOp op) {
    getDocument().consume(op);
  }

  /**
   * Renders the document, keeping that rendering live until
   * {@link #stopRendering() stopped}.
   *
   * @param registries registries for rendering
   */
  public void startRendering(Registries registries, LogicalPanel logicalPanel) {
    if (document == null) {
      loadWith(registries);
    } else {
      document.setRegistries(registries);
    }
    document.setInteractive(logicalPanel);
    // ContentDocument does not render synchronously, so we have to force it
    // to finish, rather than reveal half-rendered content at the end of the
    // event cycle.
    AnnotationPainter.flush(document.getContext());
  }

  /**
   * Stops rendering the document.
   */
  public void stopRendering() {
    document.setShelved();
  }

  @Override
  public boolean flush(Runnable resume) {
    return document.flush(resume);
  }
}
