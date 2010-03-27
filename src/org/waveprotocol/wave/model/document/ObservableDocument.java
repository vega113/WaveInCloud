// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

/**
 * Observable document without generics
 */
public interface ObservableDocument extends
    ObservableMutableDocument<Doc.N, Doc.E, Doc.T>, Document {

}
