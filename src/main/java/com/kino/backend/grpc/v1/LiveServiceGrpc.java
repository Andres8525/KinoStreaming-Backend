package com.kino.backend.grpc.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class LiveServiceGrpc {

  private LiveServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "kino.v1.LiveService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.kino.backend.grpc.v1.ListLiveStreamsRequest,
      com.kino.backend.grpc.v1.LiveStreamsSnapshot> getListLiveStreamsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListLiveStreams",
      requestType = com.kino.backend.grpc.v1.ListLiveStreamsRequest.class,
      responseType = com.kino.backend.grpc.v1.LiveStreamsSnapshot.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.kino.backend.grpc.v1.ListLiveStreamsRequest,
      com.kino.backend.grpc.v1.LiveStreamsSnapshot> getListLiveStreamsMethod() {
    io.grpc.MethodDescriptor<com.kino.backend.grpc.v1.ListLiveStreamsRequest, com.kino.backend.grpc.v1.LiveStreamsSnapshot> getListLiveStreamsMethod;
    if ((getListLiveStreamsMethod = LiveServiceGrpc.getListLiveStreamsMethod) == null) {
      synchronized (LiveServiceGrpc.class) {
        if ((getListLiveStreamsMethod = LiveServiceGrpc.getListLiveStreamsMethod) == null) {
          LiveServiceGrpc.getListLiveStreamsMethod = getListLiveStreamsMethod =
              io.grpc.MethodDescriptor.<com.kino.backend.grpc.v1.ListLiveStreamsRequest, com.kino.backend.grpc.v1.LiveStreamsSnapshot>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListLiveStreams"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.kino.backend.grpc.v1.ListLiveStreamsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.kino.backend.grpc.v1.LiveStreamsSnapshot.getDefaultInstance()))
              .setSchemaDescriptor(new LiveServiceMethodDescriptorSupplier("ListLiveStreams"))
              .build();
        }
      }
    }
    return getListLiveStreamsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.kino.backend.grpc.v1.WatchLiveStreamsRequest,
      com.kino.backend.grpc.v1.LiveStreamsSnapshot> getWatchLiveStreamsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WatchLiveStreams",
      requestType = com.kino.backend.grpc.v1.WatchLiveStreamsRequest.class,
      responseType = com.kino.backend.grpc.v1.LiveStreamsSnapshot.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.kino.backend.grpc.v1.WatchLiveStreamsRequest,
      com.kino.backend.grpc.v1.LiveStreamsSnapshot> getWatchLiveStreamsMethod() {
    io.grpc.MethodDescriptor<com.kino.backend.grpc.v1.WatchLiveStreamsRequest, com.kino.backend.grpc.v1.LiveStreamsSnapshot> getWatchLiveStreamsMethod;
    if ((getWatchLiveStreamsMethod = LiveServiceGrpc.getWatchLiveStreamsMethod) == null) {
      synchronized (LiveServiceGrpc.class) {
        if ((getWatchLiveStreamsMethod = LiveServiceGrpc.getWatchLiveStreamsMethod) == null) {
          LiveServiceGrpc.getWatchLiveStreamsMethod = getWatchLiveStreamsMethod =
              io.grpc.MethodDescriptor.<com.kino.backend.grpc.v1.WatchLiveStreamsRequest, com.kino.backend.grpc.v1.LiveStreamsSnapshot>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WatchLiveStreams"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.kino.backend.grpc.v1.WatchLiveStreamsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.kino.backend.grpc.v1.LiveStreamsSnapshot.getDefaultInstance()))
              .setSchemaDescriptor(new LiveServiceMethodDescriptorSupplier("WatchLiveStreams"))
              .build();
        }
      }
    }
    return getWatchLiveStreamsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static LiveServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LiveServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LiveServiceStub>() {
        @java.lang.Override
        public LiveServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LiveServiceStub(channel, callOptions);
        }
      };
    return LiveServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static LiveServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LiveServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LiveServiceBlockingV2Stub>() {
        @java.lang.Override
        public LiveServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LiveServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return LiveServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static LiveServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LiveServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LiveServiceBlockingStub>() {
        @java.lang.Override
        public LiveServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LiveServiceBlockingStub(channel, callOptions);
        }
      };
    return LiveServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static LiveServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LiveServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LiveServiceFutureStub>() {
        @java.lang.Override
        public LiveServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LiveServiceFutureStub(channel, callOptions);
        }
      };
    return LiveServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void listLiveStreams(com.kino.backend.grpc.v1.ListLiveStreamsRequest request,
        io.grpc.stub.StreamObserver<com.kino.backend.grpc.v1.LiveStreamsSnapshot> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListLiveStreamsMethod(), responseObserver);
    }

    /**
     */
    default void watchLiveStreams(com.kino.backend.grpc.v1.WatchLiveStreamsRequest request,
        io.grpc.stub.StreamObserver<com.kino.backend.grpc.v1.LiveStreamsSnapshot> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getWatchLiveStreamsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service LiveService.
   */
  public static abstract class LiveServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return LiveServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service LiveService.
   */
  public static final class LiveServiceStub
      extends io.grpc.stub.AbstractAsyncStub<LiveServiceStub> {
    private LiveServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LiveServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LiveServiceStub(channel, callOptions);
    }

    /**
     */
    public void listLiveStreams(com.kino.backend.grpc.v1.ListLiveStreamsRequest request,
        io.grpc.stub.StreamObserver<com.kino.backend.grpc.v1.LiveStreamsSnapshot> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListLiveStreamsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void watchLiveStreams(com.kino.backend.grpc.v1.WatchLiveStreamsRequest request,
        io.grpc.stub.StreamObserver<com.kino.backend.grpc.v1.LiveStreamsSnapshot> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getWatchLiveStreamsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service LiveService.
   */
  public static final class LiveServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<LiveServiceBlockingV2Stub> {
    private LiveServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LiveServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LiveServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     */
    public com.kino.backend.grpc.v1.LiveStreamsSnapshot listLiveStreams(com.kino.backend.grpc.v1.ListLiveStreamsRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListLiveStreamsMethod(), getCallOptions(), request);
    }

    /**
     */
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/10918")
    public io.grpc.stub.BlockingClientCall<?, com.kino.backend.grpc.v1.LiveStreamsSnapshot>
        watchLiveStreams(com.kino.backend.grpc.v1.WatchLiveStreamsRequest request) {
      return io.grpc.stub.ClientCalls.blockingV2ServerStreamingCall(
          getChannel(), getWatchLiveStreamsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service LiveService.
   */
  public static final class LiveServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<LiveServiceBlockingStub> {
    private LiveServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LiveServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LiveServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.kino.backend.grpc.v1.LiveStreamsSnapshot listLiveStreams(com.kino.backend.grpc.v1.ListLiveStreamsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListLiveStreamsMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.kino.backend.grpc.v1.LiveStreamsSnapshot> watchLiveStreams(
        com.kino.backend.grpc.v1.WatchLiveStreamsRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getWatchLiveStreamsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service LiveService.
   */
  public static final class LiveServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<LiveServiceFutureStub> {
    private LiveServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LiveServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LiveServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.kino.backend.grpc.v1.LiveStreamsSnapshot> listLiveStreams(
        com.kino.backend.grpc.v1.ListLiveStreamsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListLiveStreamsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_LIVE_STREAMS = 0;
  private static final int METHODID_WATCH_LIVE_STREAMS = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_LIST_LIVE_STREAMS:
          serviceImpl.listLiveStreams((com.kino.backend.grpc.v1.ListLiveStreamsRequest) request,
              (io.grpc.stub.StreamObserver<com.kino.backend.grpc.v1.LiveStreamsSnapshot>) responseObserver);
          break;
        case METHODID_WATCH_LIVE_STREAMS:
          serviceImpl.watchLiveStreams((com.kino.backend.grpc.v1.WatchLiveStreamsRequest) request,
              (io.grpc.stub.StreamObserver<com.kino.backend.grpc.v1.LiveStreamsSnapshot>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getListLiveStreamsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.kino.backend.grpc.v1.ListLiveStreamsRequest,
              com.kino.backend.grpc.v1.LiveStreamsSnapshot>(
                service, METHODID_LIST_LIVE_STREAMS)))
        .addMethod(
          getWatchLiveStreamsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.kino.backend.grpc.v1.WatchLiveStreamsRequest,
              com.kino.backend.grpc.v1.LiveStreamsSnapshot>(
                service, METHODID_WATCH_LIVE_STREAMS)))
        .build();
  }

  private static abstract class LiveServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    LiveServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.kino.backend.grpc.v1.KinoProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("LiveService");
    }
  }

  private static final class LiveServiceFileDescriptorSupplier
      extends LiveServiceBaseDescriptorSupplier {
    LiveServiceFileDescriptorSupplier() {}
  }

  private static final class LiveServiceMethodDescriptorSupplier
      extends LiveServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    LiveServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (LiveServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new LiveServiceFileDescriptorSupplier())
              .addMethod(getListLiveStreamsMethod())
              .addMethod(getWatchLiveStreamsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
