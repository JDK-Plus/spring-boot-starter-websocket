// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: message.proto

package plus.jdk.websocket.protoc;

public final class WsBroadcastMessage {
  private WsBroadcastMessage() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_plus_jdk_websocket_protoc_WsMessage_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_plus_jdk_websocket_protoc_WsMessage_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\rmessage.proto\022\031plus.jdk.websocket.prot" +
      "oc\"\241\001\n\tWsMessage\022\020\n\003uid\030\001 \001(\tH\000\210\001\001\022\021\n\004pa" +
      "th\030\002 \001(\tH\001\210\001\001\022\021\n\004data\030\003 \001(\014H\002\210\001\001\0229\n\004type" +
      "\030\004 \001(\0162&.plus.jdk.websocket.protoc.Messa" +
      "geTypeH\003\210\001\001B\006\n\004_uidB\007\n\005_pathB\007\n\005_dataB\007\n" +
      "\005_type*=\n\013MessageType\022\025\n\021MESSAGE_TYPE_TE" +
      "XT\020\000\022\027\n\023MESSAGE_TYPE_BINARY\020\001B3\n\031plus.jd" +
      "k.websocket.protocB\022WsBroadcastMessageH\002" +
      "P\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_plus_jdk_websocket_protoc_WsMessage_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_plus_jdk_websocket_protoc_WsMessage_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_plus_jdk_websocket_protoc_WsMessage_descriptor,
        new java.lang.String[] { "Uid", "Path", "Data", "Type", "Uid", "Path", "Data", "Type", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
