public $if(m.inner)$static$endif$ final class $m.javaType$
    implements $m.messageType$Message, GsonSerializable {

  $m.messages: {nested|$class(m=nested)$}$

  private $m.protoType$ proto = null;
  private $m.protoType$.Builder protoBuilder = $m.protoType$.newBuilder();

  public $m.javaType$() {
  }

  public $m.javaType$($m.protoType$ proto) {
    this.proto = proto;
  }

  public $m.javaType$($m.messageType$Message message) {
    copyFrom(message);
  }

  public $m.protoType$ getPB() {
    lazilySwitchToProto();
    return proto;
  }

  public void setPB($m.protoType$ proto) {
    this.proto = proto;
    this.protoBuilder = null;
  }

  @Override
  public void copyFrom($m.messageType$Message message) {
    $m.fields: {f|$copyField(f=f, target="message")$}$
  }

  $m.fields: {f|$beans(m=m, f=f)$}$

  $m.fieldTypes: {t|
    $if (t.message)$
      $messageHelpers(t=t)$
    $elseif (t.enum)$
      $enumHelpers(t=t)$
    $endif$
  }$

  private void lazilySwitchToProto() {
    if (proto == null) {
      proto = protoBuilder.build();
      protoBuilder = null;
    }
  }

  private void lazilySwitchToProtoBuilder() {
    if (protoBuilder == null) {
      protoBuilder = (proto == null)
          ? $m.protoType$.newBuilder()
          : $m.protoType$.newBuilder(proto);
      proto = null;
    }
  }

  private void invalidateAll() {
    proto = null;
    protoBuilder = null;
  }

  @Override
  public JsonElement toGson(RawStringData raw, Gson gson) {
    JsonObject json = new JsonObject();
    $m.fields: {f|$toGsonField(f=f, target="json")$}$
    return json;
  }

  @Override
  public void fromGson(JsonElement json, Gson gson, RawStringData raw) throws GsonException {
    JsonObject jsonObject = json.getAsJsonObject();
    // NOTE: always check with has(...) as the json might not have all required
    // fields set; however these (obviously) will need to be set by other means
    // before accessing this object.
    invalidateAll();
    $m.fields: {f|$fromGsonField(f=f, target="jsonObject")$}$
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof $m.javaType$) {
      return getPB().equals((($m.javaType$) o).getPB());
    } else {
      return false;
    }
  }

  @Override
  public boolean isEqualTo(Object o) {
    if (equals(o)) {
      return true;
    } else if (o instanceof $m.messageType$Message) {
      return $m.messageType$Messages.isEqual(this, ($m.messageType$Message) o);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return getPB().hashCode();
  }

  @Override
  public String toString() {
    return getPB().toString();
  }
}
