// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.operation;

import org.waveprotocol.wave.model.document.operation.DocOp.IsDocOp;

/**
 * Sinks everything and provides a DocInitialization.
 *
 * @author anorth@google.com (Alex North)
 */
public interface SuperSink extends ModifiableDocument, NindoSink, IsDocOp {

}
