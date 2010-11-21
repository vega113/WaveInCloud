public $if(m.inner)$static$endif$ final class $m.javaType$ {

  $m.messages: {nested|$class(m=nested)$}$

  private $m.javaType$() {
  }

  /**
   * Structurally compares one message to another.
   *
   * @return whether two messages are structurally equal
   */
  public static boolean isEqual($m.messageType$Message m1, $m.messageType$Message m2) {
    $m.fields: {f|$returnFalseIfNotEqual(f=f, target1="m1", target2="m2")$}$
    return true;
  }

  /**
   * Compares two lists of $m.messageType$Messages using isEqual.
   */
  private static boolean areAllEqual(List<? extends $m.messageType$Message> m1,
      List<? extends $m.messageType$Message> m2) {
    if (m1.size() != m2.size()) return false;
    Iterator<? extends $m.messageType$Message> i1 = m1.iterator();
    Iterator<? extends $m.messageType$Message> i2 = m2.iterator();
    while (i1.hasNext()) {
      if (!isEqual(i1.next(), i2.next())) return false;
    }
    return true;
  }

  /**
   * Structurally calculates the hash code of a message.
   *
   * @return the hash code of a message
   */
  public static int getHashCode($m.messageType$Message message) {
    int result = 1;
    $m.fields: {f|$addHashCode(f=f, shift="31", result="result", target="message")$}$
    return result;
  }
}
