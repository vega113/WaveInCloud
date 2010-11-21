public $if(m.inner)$static$endif$ class $m.javaType$ implements $m.messageType$Message {

  $m.messages: {nested|$class(m=nested)$}$

  $m.fields: {f|$fieldDeclaration(f=f)$}$

  public $m.javaType$() {
  }

  public $m.javaType$($m.messageType$Message message) {
    copyFrom(message);
  }

  @Override
  public void copyFrom($m.messageType$Message message) {
    $m.fields: {f|$copyField(f=f, target="message")$}$
  }

  $m.fields: {f|$beans(m=m, f=f)$}$

  /**
   * Provided to subclasses to clear all fields, for example when deserializing.
   */
  protected void reset() {
    $m.fields: {f|$clearField(f=f)$}$
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof $m.javaType$) && isEqualTo(o);
  }

  @Override
  public boolean isEqualTo(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof $m.messageType$Message) {
      return $m.messageType$Messages.isEqual(this, ($m.messageType$Message) o);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return $m.messageType$Messages.getHashCode(this);
  }
}
