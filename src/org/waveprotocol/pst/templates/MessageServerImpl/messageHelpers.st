/**
 * Helper to get or create a $t.javaType$ from a $t.messageType$Message.
 */
private $t.javaType$ getOrCreate$t.javaType$($t.messageType$Message message) {
    if (message instanceof $t.javaType$) {
      return ($t.javaType$) message;
    } else {
      $t.javaType$ messageImpl = new $t.javaType$();
      messageImpl.copyFrom(message);
      return messageImpl;
    }
}
