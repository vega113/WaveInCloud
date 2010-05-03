// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.MutableDocumentImpl;
import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Implementation of Document in terms of generic inner objects
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class DocumentImpl extends MutableDocumentImpl<Doc.N, Doc.E, Doc.T> implements Document {

  /**
   * @see MutableDocumentImpl#MutableDocumentImpl(OperationSequencer, ReadableWDocument)
   */
  // Unfortunately, java does not permit <N extends N, E extends N & Doc.E, ...>
  // which would be required to make this typesafe.
  @SuppressWarnings("unchecked")
  public DocumentImpl(OperationSequencer<Nindo> sequencer,
      ReadableWDocument document) {
    super(sequencer, document);

    Preconditions.checkArgument(document.getDocumentElement() instanceof Doc.E,
        "Document and sequencer must be for nodes of the Doc.* variety");
  }
}
