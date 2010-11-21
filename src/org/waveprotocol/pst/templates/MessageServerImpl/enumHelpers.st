/**
 * Helper to get a proto enum from a pojo enum.
 */
private $t.protoType$.$t.javaType$ toProtoEnum$t.javaType$($t.javaType$ value) {
  return $t.protoType$.$t.javaType$.valueOf(value.getValue());
}

/**
 * Helper to get a pojo enum from a proto enum.
 */
private $t.javaType$ toPojoEnum$t.javaType$($t.protoType$.$t.javaType$ value) {
  return ProtoEnums.valOf(value.getNumber(), $t.javaType$.values());
}
