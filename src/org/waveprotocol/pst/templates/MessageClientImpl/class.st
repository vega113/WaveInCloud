public $if(m.inner)$static$endif$ final class $m.javaType$ extends JsonMessage
    implements $m.messageType$Message {

  $m.messages: {nested|$class(m=nested)$}$

  $m.fields: {f|
    private static final String key$f.capName$ = "$f.number$";
  }$

  protected $m.javaType$() {
  }

  public static $m.javaType$ create() {
    $m.javaType$ instance = ($m.javaType$) JsonMessage.createJsonMessage();
    // TODO(kalman): enable.
    /*
    if (LogLevel.showDebug()) {
      instance.registerNative$m.messageType$MessageToString();
    }
    */
    
    // Force all lists to start with an empty list rather than no property for
    // the list. This is so that the native JS equality works, since (obviously)
    // {} != {"foo": []} while in the context of messages they should be.
    $m.fields: {f|
      $if (f.repeated)$
        instance.clear$f.capName$();
      $endif$
    }$
    
    return instance;
  }

  @Override
  public void copyFrom($m.messageType$Message message) {
    super.copyFrom(($m.javaType$) message);
  }

  $m.fields: {f|$beans(m=m, f=f)$}$

  public native void registerNative$m.messageType$MessageToString() /*-{
    // TODO(kalman): do something here?
  }-*/;
  
  @Override
  public boolean isEqualTo(Object o) {
    if (o instanceof $m.javaType$) {
      return nativeIsEqualTo(o);
    } else if (o instanceof $m.messageType$Message) {
      return $m.messageType$Messages.isEqual(this, ($m.messageType$Message) o);
    } else {
      return false;
    }
  }
}
