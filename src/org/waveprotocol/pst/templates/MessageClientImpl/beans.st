$if (f.optional)$

  @Override
  public boolean has$f.capName$() {
    return hasProperty(this, key$f.capName$);
  }

  @Override
  public void clear$f.capName$() {
    if (hasProperty(this, key$f.capName$)) {
      deleteProperty(this, key$f.capName$);
    }
  }

$endif$

$if (f.repeated)$

  @Override
  public List<$f.boxedJavaType$> $f.getter$() {
    initArray(this, key$f.capName$);
    List<$f.boxedJavaType$> list = new ArrayList<$f.boxedJavaType$>();
    for (int i = 0; i < $f.getter$Size(); i++) {
      list.add($f.getter$(i));
    }
    return list;
  }

  $if (f.message)$
    @Override
    public void addAll$f.capName$(List<? extends $f.messageType$Message> messages) {
      for ($f.messageType$Message message : messages) {
      	append$f.capName$(message);
      }
    }
  $else$
    @Override
    public void addAll$f.capName$(List<$f.boxedJavaType$> values) {
      for ($f.javaType$ value : values) {
        append$f.capName$(value);
      }
    }
  $endif$

  @Override
  @SuppressWarnings("unchecked")
  public $f.javaType$ $f.getter$(int n) {
    initArray(this, key$f.capName$);
    $if (f.message)$
      JsArray<$f.javaType$> array = (JsArray<$f.javaType$>) getPropertyAsObject(this, key$f.capName$);
    $elseif (f.enum)$
      JsArrayInteger array = (JsArrayInteger) getPropertyAsObject(this, key$f.capName$);
    $else$
      JsArray$f.boxedJavaType$ array = (JsArray$f.boxedJavaType$) getPropertyAsObject(this, key$f.capName$);
    $endif$
    if (n < 0) throw new IllegalArgumentException("index " + n + " < 0");
    if (array.length() <= n) throw new IllegalArgumentException("index " + n + ">= array length " + array.length());
    $if (f.enum)$
      return ProtoEnums.valOf(array.get(n), $f.javaType$.values());
    $else$
      return array.get(n);
    $endif$
  }

  $if (f.message)$
    @Override
    @SuppressWarnings("unchecked")
    public void $f.setter$(int n, $f.messageType$Message message) {
      if (n < 0) throw new IllegalArgumentException("index " + n + " < 0");
      initArray(this, key$f.capName$);
      ((JsArray<$f.javaType$>) getPropertyAsObject(this, key$f.capName$)).set(n, ($f.javaType$) message);
    }
  $elseif (f.enum)$
    @Override
    public void $f.setter$(int n, $f.javaType$ enumValue) {
      if (n < 0) throw new IllegalArgumentException("index " + n + " < 0");
      initArray(this, key$f.capName$);
      ((JsArrayInteger) getPropertyAsObject(this, key$f.capName$)).set(n, enumValue.getValue());
    }
  $else$
    @Override
    public void $f.setter$(int n, $f.javaType$ value) {
      if (n < 0) throw new IllegalArgumentException("index " + n + " < 0");
      initArray(this, key$f.capName$);
      ((JsArray$f.boxedJavaType$) getPropertyAsObject(this, key$f.capName$)).set(n, value);
    }
  $endif$

  @Override
  public int $f.getter$Size() {
    return hasProperty(this, key$f.capName$) ? ((JsArray<?>) getPropertyAsObject(this, key$f.capName$)).length() : 0;
  }

  $if (f.message)$
    @Override
    @SuppressWarnings("unchecked")
    public void append$f.capName$($f.messageType$Message message) {
      initArray(this, key$f.capName$);
      ((JsArray<$f.javaType$>) getPropertyAsObject(this, key$f.capName$)).push(($f.javaType$) message);
    }
  $elseif (f.enum)$
    @Override
    public void append$f.capName$($f.javaType$ enumValue) {
      initArray(this, key$f.capName$);
      ((JsArrayInteger) getPropertyAsObject(this, key$f.capName$)).push(enumValue.getValue());
    }
  $else$
    @Override
    public void append$f.capName$($f.javaType$ value) {
      initArray(this, key$f.capName$);
      ((JsArray$f.boxedJavaType$) getPropertyAsObject(this, key$f.capName$)).push(value);
    }
  $endif$

  @Override
  public void clear$f.capName$() {
    clearArray(this, key$f.capName$);
  }

$else$

  @Override
  $if (f.message)$
    public $f.messageType$Message $f.getter$() {
      return hasProperty(this, key$f.capName$) ? (($f.javaType$) getPropertyAsObject(this, key$f.capName$)) : null;
    }
  $else$
    public $f.javaType$ $f.getter$() {
      $if (f.enum)$
        return hasProperty(this, key$f.capName$) ? ProtoEnums.valOf(getPropertyAsInteger(this, key$f.capName$), $f.javaType$.values()) : null;
      $else$
        return hasProperty(this, key$f.capName$) ? getPropertyAs$f.boxedJavaType$(this, key$f.capName$) : $f.defaultValue$;
      $endif$
    }
  $endif$

  $if (f.message)$
    @Override
    public void $f.setter$($f.messageType$Message message) {
      setPropertyAsObject(this, key$f.capName$, ($f.javaType$) message);
    }
  $elseif (f.enum)$
    @Override
    public void $f.setter$($f.javaType$ enumValue) {
      setPropertyAsInteger(this, key$f.capName$, enumValue.getValue());
    }
  $else$
    @Override
    public void $f.setter$($f.javaType$ value) {
      setPropertyAs$f.boxedJavaType$(this, key$f.capName$, value); 
    }
  $endif$

$endif$
