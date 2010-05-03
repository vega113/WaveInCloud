// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;

/**
 * Initial stab at what a document where local-only changes can be made might
 * look like
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface LocalDocument<N, E extends N, T extends N> extends WritableLocalDocument<N, E, T>,
    ReadableDocument<N, E, T> {
}
