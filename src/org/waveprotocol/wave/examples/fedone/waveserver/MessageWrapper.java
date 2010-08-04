package org.waveprotocol.wave.examples.fedone.waveserver;

/**
 * A simple message wrapper that bundles a json string with a version,
 * sequence number, and type information.
 */
public class MessageWrapper {
  public int version;
  public long sequenceNumber;
  public String messageType;
  public String messageJson;

  public MessageWrapper() {
    // no-args constructor
  }
  public MessageWrapper(int version, long sequenceNumber, String messageType,
      String messageJson) {
    this.version = version;
    this.sequenceNumber = sequenceNumber;
    this.messageType = messageType;
    this.messageJson = messageJson;
  }
}
