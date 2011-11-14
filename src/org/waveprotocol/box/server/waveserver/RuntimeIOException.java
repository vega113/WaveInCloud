package org.waveprotocol.box.server.waveserver;

import java.io.IOException;

/**
 * Wraps an {@link IOException} in a {@link RuntimeException}.
 */
@SuppressWarnings("serial")
class RuntimeIOException extends RuntimeException {
  private final IOException cause;

  public RuntimeIOException(IOException cause) {
    super(cause);
    this.cause = cause;
  }

  public IOException getIOException() {
    return cause;
  }
}