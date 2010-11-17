// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: org/waveprotocol/box/server/persistence/protos/ProtoDeltaStore.proto

package org.waveprotocol.box.server.persistence.protos;

public final class ProtoDeltaStoreData {
  private ProtoDeltaStoreData() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public static final class ProtoTransformedWaveletDelta extends
      com.google.protobuf.GeneratedMessage {
    // Use ProtoTransformedWaveletDelta.newBuilder() to construct.
    private ProtoTransformedWaveletDelta() {
      initFields();
    }
    private ProtoTransformedWaveletDelta(boolean noInit) {}
    
    private static final ProtoTransformedWaveletDelta defaultInstance;
    public static ProtoTransformedWaveletDelta getDefaultInstance() {
      return defaultInstance;
    }
    
    public ProtoTransformedWaveletDelta getDefaultInstanceForType() {
      return defaultInstance;
    }
    
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.internal_static_protodeltastore_ProtoTransformedWaveletDelta_descriptor;
    }
    
    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.internal_static_protodeltastore_ProtoTransformedWaveletDelta_fieldAccessorTable;
    }
    
    // required string author = 1;
    public static final int AUTHOR_FIELD_NUMBER = 1;
    private boolean hasAuthor;
    private java.lang.String author_ = "";
    public boolean hasAuthor() { return hasAuthor; }
    public java.lang.String getAuthor() { return author_; }
    
    // required .federation.ProtocolHashedVersion resulting_version = 2;
    public static final int RESULTING_VERSION_FIELD_NUMBER = 2;
    private boolean hasResultingVersion;
    private org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion resultingVersion_;
    public boolean hasResultingVersion() { return hasResultingVersion; }
    public org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion getResultingVersion() { return resultingVersion_; }
    
    // required int64 application_timestamp = 3;
    public static final int APPLICATION_TIMESTAMP_FIELD_NUMBER = 3;
    private boolean hasApplicationTimestamp;
    private long applicationTimestamp_ = 0L;
    public boolean hasApplicationTimestamp() { return hasApplicationTimestamp; }
    public long getApplicationTimestamp() { return applicationTimestamp_; }
    
    // repeated .federation.ProtocolWaveletOperation operation = 4;
    public static final int OPERATION_FIELD_NUMBER = 4;
    private java.util.List<org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation> operation_ =
      java.util.Collections.emptyList();
    public java.util.List<org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation> getOperationList() {
      return operation_;
    }
    public int getOperationCount() { return operation_.size(); }
    public org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation getOperation(int index) {
      return operation_.get(index);
    }
    
    private void initFields() {
      resultingVersion_ = org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion.getDefaultInstance();
    }
    public final boolean isInitialized() {
      if (!hasAuthor) return false;
      if (!hasResultingVersion) return false;
      if (!hasApplicationTimestamp) return false;
      if (!getResultingVersion().isInitialized()) return false;
      for (org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation element : getOperationList()) {
        if (!element.isInitialized()) return false;
      }
      return true;
    }
    
    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      getSerializedSize();
      if (hasAuthor()) {
        output.writeString(1, getAuthor());
      }
      if (hasResultingVersion()) {
        output.writeMessage(2, getResultingVersion());
      }
      if (hasApplicationTimestamp()) {
        output.writeInt64(3, getApplicationTimestamp());
      }
      for (org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation element : getOperationList()) {
        output.writeMessage(4, element);
      }
      getUnknownFields().writeTo(output);
    }
    
    private int memoizedSerializedSize = -1;
    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;
    
      size = 0;
      if (hasAuthor()) {
        size += com.google.protobuf.CodedOutputStream
          .computeStringSize(1, getAuthor());
      }
      if (hasResultingVersion()) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(2, getResultingVersion());
      }
      if (hasApplicationTimestamp()) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(3, getApplicationTimestamp());
      }
      for (org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation element : getOperationList()) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(4, element);
      }
      size += getUnknownFields().getSerializedSize();
      memoizedSerializedSize = size;
      return size;
    }
    
    public static org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data).buildParsed();
    }
    public static org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return newBuilder().mergeFrom(data, extensionRegistry)
               .buildParsed();
    }
    public static org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    public static org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      Builder builder = newBuilder();
      if (builder.mergeDelimitedFrom(input, extensionRegistry)) {
        return builder.buildParsed();
      } else {
        return null;
      }
    }
    public static org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input).buildParsed();
    }
    public static org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return newBuilder().mergeFrom(input, extensionRegistry)
               .buildParsed();
    }
    
    public static Builder newBuilder() { return Builder.create(); }
    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder(org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta prototype) {
      return newBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() { return newBuilder(this); }
    
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> {
      private org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta result;
      
      // Construct using org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta.newBuilder()
      private Builder() {}
      
      private static Builder create() {
        Builder builder = new Builder();
        builder.result = new org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta();
        return builder;
      }
      
      protected org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta internalGetResult() {
        return result;
      }
      
      public Builder clear() {
        if (result == null) {
          throw new IllegalStateException(
            "Cannot call clear() after build().");
        }
        result = new org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta();
        return this;
      }
      
      public Builder clone() {
        return create().mergeFrom(result);
      }
      
      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta.getDescriptor();
      }
      
      public org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta getDefaultInstanceForType() {
        return org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta.getDefaultInstance();
      }
      
      public boolean isInitialized() {
        return result.isInitialized();
      }
      public org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta build() {
        if (result != null && !isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return buildPartial();
      }
      
      private org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta buildParsed()
          throws com.google.protobuf.InvalidProtocolBufferException {
        if (!isInitialized()) {
          throw newUninitializedMessageException(
            result).asInvalidProtocolBufferException();
        }
        return buildPartial();
      }
      
      public org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta buildPartial() {
        if (result == null) {
          throw new IllegalStateException(
            "build() has already been called on this Builder.");
        }
        if (result.operation_ != java.util.Collections.EMPTY_LIST) {
          result.operation_ =
            java.util.Collections.unmodifiableList(result.operation_);
        }
        org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta returnMe = result;
        result = null;
        return returnMe;
      }
      
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta) {
          return mergeFrom((org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }
      
      public Builder mergeFrom(org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta other) {
        if (other == org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta.getDefaultInstance()) return this;
        if (other.hasAuthor()) {
          setAuthor(other.getAuthor());
        }
        if (other.hasResultingVersion()) {
          mergeResultingVersion(other.getResultingVersion());
        }
        if (other.hasApplicationTimestamp()) {
          setApplicationTimestamp(other.getApplicationTimestamp());
        }
        if (!other.operation_.isEmpty()) {
          if (result.operation_.isEmpty()) {
            result.operation_ = new java.util.ArrayList<org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation>();
          }
          result.operation_.addAll(other.operation_);
        }
        this.mergeUnknownFields(other.getUnknownFields());
        return this;
      }
      
      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder(
            this.getUnknownFields());
        while (true) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              this.setUnknownFields(unknownFields.build());
              return this;
            default: {
              if (!parseUnknownField(input, unknownFields,
                                     extensionRegistry, tag)) {
                this.setUnknownFields(unknownFields.build());
                return this;
              }
              break;
            }
            case 10: {
              setAuthor(input.readString());
              break;
            }
            case 18: {
              org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion.Builder subBuilder = org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion.newBuilder();
              if (hasResultingVersion()) {
                subBuilder.mergeFrom(getResultingVersion());
              }
              input.readMessage(subBuilder, extensionRegistry);
              setResultingVersion(subBuilder.buildPartial());
              break;
            }
            case 24: {
              setApplicationTimestamp(input.readInt64());
              break;
            }
            case 34: {
              org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation.Builder subBuilder = org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation.newBuilder();
              input.readMessage(subBuilder, extensionRegistry);
              addOperation(subBuilder.buildPartial());
              break;
            }
          }
        }
      }
      
      
      // required string author = 1;
      public boolean hasAuthor() {
        return result.hasAuthor();
      }
      public java.lang.String getAuthor() {
        return result.getAuthor();
      }
      public Builder setAuthor(java.lang.String value) {
        if (value == null) {
    throw new NullPointerException();
  }
  result.hasAuthor = true;
        result.author_ = value;
        return this;
      }
      public Builder clearAuthor() {
        result.hasAuthor = false;
        result.author_ = getDefaultInstance().getAuthor();
        return this;
      }
      
      // required .federation.ProtocolHashedVersion resulting_version = 2;
      public boolean hasResultingVersion() {
        return result.hasResultingVersion();
      }
      public org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion getResultingVersion() {
        return result.getResultingVersion();
      }
      public Builder setResultingVersion(org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion value) {
        if (value == null) {
          throw new NullPointerException();
        }
        result.hasResultingVersion = true;
        result.resultingVersion_ = value;
        return this;
      }
      public Builder setResultingVersion(org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion.Builder builderForValue) {
        result.hasResultingVersion = true;
        result.resultingVersion_ = builderForValue.build();
        return this;
      }
      public Builder mergeResultingVersion(org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion value) {
        if (result.hasResultingVersion() &&
            result.resultingVersion_ != org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion.getDefaultInstance()) {
          result.resultingVersion_ =
            org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion.newBuilder(result.resultingVersion_).mergeFrom(value).buildPartial();
        } else {
          result.resultingVersion_ = value;
        }
        result.hasResultingVersion = true;
        return this;
      }
      public Builder clearResultingVersion() {
        result.hasResultingVersion = false;
        result.resultingVersion_ = org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion.getDefaultInstance();
        return this;
      }
      
      // required int64 application_timestamp = 3;
      public boolean hasApplicationTimestamp() {
        return result.hasApplicationTimestamp();
      }
      public long getApplicationTimestamp() {
        return result.getApplicationTimestamp();
      }
      public Builder setApplicationTimestamp(long value) {
        result.hasApplicationTimestamp = true;
        result.applicationTimestamp_ = value;
        return this;
      }
      public Builder clearApplicationTimestamp() {
        result.hasApplicationTimestamp = false;
        result.applicationTimestamp_ = 0L;
        return this;
      }
      
      // repeated .federation.ProtocolWaveletOperation operation = 4;
      public java.util.List<org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation> getOperationList() {
        return java.util.Collections.unmodifiableList(result.operation_);
      }
      public int getOperationCount() {
        return result.getOperationCount();
      }
      public org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation getOperation(int index) {
        return result.getOperation(index);
      }
      public Builder setOperation(int index, org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation value) {
        if (value == null) {
          throw new NullPointerException();
        }
        result.operation_.set(index, value);
        return this;
      }
      public Builder setOperation(int index, org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation.Builder builderForValue) {
        result.operation_.set(index, builderForValue.build());
        return this;
      }
      public Builder addOperation(org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation value) {
        if (value == null) {
          throw new NullPointerException();
        }
        if (result.operation_.isEmpty()) {
          result.operation_ = new java.util.ArrayList<org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation>();
        }
        result.operation_.add(value);
        return this;
      }
      public Builder addOperation(org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation.Builder builderForValue) {
        if (result.operation_.isEmpty()) {
          result.operation_ = new java.util.ArrayList<org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation>();
        }
        result.operation_.add(builderForValue.build());
        return this;
      }
      public Builder addAllOperation(
          java.lang.Iterable<? extends org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation> values) {
        if (result.operation_.isEmpty()) {
          result.operation_ = new java.util.ArrayList<org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation>();
        }
        super.addAll(values, result.operation_);
        return this;
      }
      public Builder clearOperation() {
        result.operation_ = java.util.Collections.emptyList();
        return this;
      }
      
      // @@protoc_insertion_point(builder_scope:protodeltastore.ProtoTransformedWaveletDelta)
    }
    
    static {
      defaultInstance = new ProtoTransformedWaveletDelta(true);
      org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.internalForceInit();
      defaultInstance.initFields();
    }
    
    // @@protoc_insertion_point(class_scope:protodeltastore.ProtoTransformedWaveletDelta)
  }
  
  private static com.google.protobuf.Descriptors.Descriptor
    internal_static_protodeltastore_ProtoTransformedWaveletDelta_descriptor;
  private static
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_protodeltastore_ProtoTransformedWaveletDelta_fieldAccessorTable;
  
  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\nDorg/waveprotocol/box/server/persistenc" +
      "e/protos/ProtoDeltaStore.proto\022\017protodel" +
      "tastore\0326org/waveprotocol/wave/federatio" +
      "n/federation.protodevel\"\304\001\n\034ProtoTransfo" +
      "rmedWaveletDelta\022\016\n\006author\030\001 \002(\t\022<\n\021resu" +
      "lting_version\030\002 \002(\0132!.federation.Protoco" +
      "lHashedVersion\022\035\n\025application_timestamp\030" +
      "\003 \002(\003\0227\n\toperation\030\004 \003(\0132$.federation.Pr" +
      "otocolWaveletOperationBE\n.org.waveprotoc" +
      "ol.box.server.persistence.protosB\023ProtoD",
      "eltaStoreData"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
      new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
        public com.google.protobuf.ExtensionRegistry assignDescriptors(
            com.google.protobuf.Descriptors.FileDescriptor root) {
          descriptor = root;
          internal_static_protodeltastore_ProtoTransformedWaveletDelta_descriptor =
            getDescriptor().getMessageTypes().get(0);
          internal_static_protodeltastore_ProtoTransformedWaveletDelta_fieldAccessorTable = new
            com.google.protobuf.GeneratedMessage.FieldAccessorTable(
              internal_static_protodeltastore_ProtoTransformedWaveletDelta_descriptor,
              new java.lang.String[] { "Author", "ResultingVersion", "ApplicationTimestamp", "Operation", },
              org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta.class,
              org.waveprotocol.box.server.persistence.protos.ProtoDeltaStoreData.ProtoTransformedWaveletDelta.Builder.class);
          return null;
        }
      };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          org.waveprotocol.wave.federation.Proto.getDescriptor(),
        }, assigner);
  }
  
  public static void internalForceInit() {}
  
  // @@protoc_insertion_point(outer_class_scope)
}