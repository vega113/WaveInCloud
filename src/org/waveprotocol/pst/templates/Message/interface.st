public interface $m.javaType$ {

  $m.enums: {e|$enum(e=e)$}$
  $m.messages: {nested|$interface(m=nested)$}$

  /**
   * Does a deep copy from message.
   */
  void copyFrom($m.javaType$ message);

  /**
   * Tests if this message is equal to another object.
   * "Equal" is recursively defined as:
   * <ul>
   * <li>both objects implement this interface,</li>
   * <li>all corresponding primitive fields of both objects have the same value, and</li>
   * <li>all corresponding nested-message fields of both objects are "equal".</li>
   * </ul>
   *
   * This is a wider class of equivalence than provided by .equals() methods.  Two
   * objects may not be .equal() to each other, but may be .isEqualTo() each other.
   */
  boolean isEqualTo(Object o);

  $m.fields: {f|$beans(f=f)$}$
}
