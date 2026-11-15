package com.kino.backend.grpc.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class CatalogServiceGrpc {

  private CatalogServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "kino.v1.CatalogService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.kino.backend.grpc.v1.ListTrendingVideosRequest,
      com.kino.backend.grpc.v1.ListTrendingVideosResponse> getListTrendingVideosMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListTrendingVideos",
      requestType = com.kino.backend.grpc.v1.ListTrendingVideosRequest.class,
      responseType = com.kino.backend.grpc.v1.ListTrendingVideosResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.kino.backend.grpc.v1.ListTrendingVideosRequest,
      com.kino.backend.grpc.v1.ListTrendingVideosResponse> getListTrendingVideosMethod() {
    io.grpc.MethodDescriptor<com.kino.backend.grpc.v1.ListTrendingVideosRequest, com.kino.backend.grpc.v1.ListTrendingVideosResponse> getListTrendingVideosMethod;
    if ((getListTrendingVideosMethod = CatalogServiceGrpc.getListTrendingVideosMethod) == null) {
      synchronized (CatalogServiceGrpc.class) {
        if ((getListTrendingVideosMethod = CatalogServiceGrpc.getListTrendingVideosMethod) == null) {
          CatalogServiceGrpc.getListTrendingVideosMethod = getListTrendingVideosMethod =
              io.grpc.MethodDescriptor.<com.kino.backend.grpc.v1.ListTrendingVideosRequest, com.kino.backend.grpc.v1.ListTrendingVideosResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListTrendingVideos"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.kino.backend.grpc.v1.ListTrendingVideosRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.kino.backend.grpc.v1.ListTrendingVideosResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CatalogServiceMethodDescriptorSupplier("ListTrendingVideos"))
              .build();
        }
      }
    }
    return getListTrendingVideosMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.kino.backend.grpc.v1.GetVideoRequest,
      com.kino.backend.grpc.v1.Video> getGetVideoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetVideo",
      requestType = com.kino.backend.grpc.v1.GetVideoRequest.class,
      responseType = com.kino.backend.grpc.v1.Video.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.kino.backend.grpc.v1.GetVideoRequest,
      com.kino.backend.grpc.v1.Video> getGetVideoMethod() {
    io.grpc.MethodDescriptor<com.kino.backend.grpc.v1.GetVideoRequest, com.kino.backend.grpc.v1.Video> getGetVideoMethod;
    if ((getGetVideoMethod = CatalogServiceGrpc.getGetVideoMethod) == null) {
      synchronized (CatalogServiceGrpc.class) {
        if ((getGetVideoMethod = CatalogServiceGrpc.getGetVideoMethod) == null) {
          CatalogServiceGrpc.getGetVideoMethod = getGetVideoMethod =
              io.grpc.MethodDescriptor.<com.kino.backend.grpc.v1.GetVideoRequest, com.kino.backend.grpc.v1.Video>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetVideo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.kino.backend.grpc.v1.GetVideoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.kino.backend.grpc.v1.Video.getDefaultInstance()))
              .setSchemaDescriptor(new CatalogServiceMethodDescriptorSupplier("GetVideo"))
              .build();
        }
      }
    }
    return getGetVideoMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CatalogServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CatalogServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CatalogServiceStub>() {
        @java.lang.Override
        public CatalogServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CatalogServiceStub(channel, callOptions);
        }
      };
    return CatalogServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static CatalogServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CatalogServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CatalogServiceBlockingV2Stub>() {
        @java.lang.Override
        public CatalogServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CatalogServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return CatalogServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CatalogServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CatalogServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CatalogServiceBlockingStub>() {
        @java.lang.Override
        public CatalogServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CatalogServiceBlockingStub(channel, callOptions);
        }
      };
    return CatalogServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static CatalogServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CatalogServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CatalogServiceFutureStub>() {
        @java.lang.Override
        public CatalogServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CatalogServiceFutureStub(channel, callOptions);
        }
      };
    return CatalogServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void listTrendingVideos(com.kino.backend.grpc.v1.ListTrendingVideosRequest request,
        io.grpc.stub.StreamObserver<com.kino.backend.grpc.v1.ListTrendingVideosResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListTrendingVideosMethod(), responseObserver);
    }

    /**
     */
    default void getVideo(com.kino.backend.grpc.v1.GetVideoRequest request,
        io.grpc.stub.StreamObserver<com.kino.backend.grpc.v1.Video> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetVideoMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service CatalogService.
   */
  public static abstract class CatalogServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return CatalogServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service CatalogService.
   */
  public static final class CatalogServiceStub
      extends io.grpc.stub.AbstractAsyncStub<CatalogServiceStub> {
    private CatalogServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CatalogServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CatalogServiceStub(channel, callOptions);
    }

    /**
     */
    public void listTrendingVideos(com.kino.backend.grpc.v1.ListTrendingVideosRequest request,
        io.grpc.stub.StreamObserver<com.kino.backend.grpc.v1.ListTrendingVideosResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListTrendingVideosMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getVideo(com.kino.backend.grpc.v1.GetVideoRequest request,
        io.grpc.stub.StreamObserver<com.kino.backend.grpc.v1.Video> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetVideoMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service CatalogService.
   */
  public static final class CatalogServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<CatalogServiceBlockingV2Stub> {
    private CatalogServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CatalogServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CatalogServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     */
    public com.kino.backend.grpc.v1.ListTrendingVideosResponse listTrendingVideos(com.kino.backend.grpc.v1.ListTrendingVideosRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getListTrendingVideosMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.kino.backend.grpc.v1.Video getVideo(com.kino.backend.grpc.v1.GetVideoRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetVideoMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service CatalogService.
   */
  public static final class CatalogServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<CatalogServiceBlockingStub> {
    private CatalogServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CatalogServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CatalogServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.kino.backend.grpc.v1.ListTrendingVideosResponse listTrendingVideos(com.kino.backend.grpc.v1.ListTrendingVideosRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListTrendingVideosMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.kino.backend.grpc.v1.Video getVideo(com.kino.backend.grpc.v1.GetVideoRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetVideoMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service CatalogService.
   */
  public static final class CatalogServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<CatalogServiceFutureStub> {
    private CatalogServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CatalogServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CatalogServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.kino.backend.grpc.v1.ListTrendingVideosResponse> listTrendingVideos(
        com.kino.backend.grpc.v1.ListTrendingVideosRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListTrendingVideosMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.kino.backend.grpc.v1.Video> getVideo(
        com.kino.backend.grpc.v1.GetVideoRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetVideoMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_TRENDING_VIDEOS = 0;
  private static final int METHODID_GET_VIDEO = 1;

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
        case METHODID_LIST_TRENDING_VIDEOS:
          serviceImpl.listTrendingVideos((com.kino.backend.grpc.v1.ListTrendingVideosRequest) request,
              (io.grpc.stub.StreamObserver<com.kino.backend.grpc.v1.ListTrendingVideosResponse>) responseObserver);
          break;
        case METHODID_GET_VIDEO:
          serviceImpl.getVideo((com.kino.backend.grpc.v1.GetVideoRequest) request,
              (io.grpc.stub.StreamObserver<com.kino.backend.grpc.v1.Video>) responseObserver);
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
          getListTrendingVideosMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.kino.backend.grpc.v1.ListTrendingVideosRequest,
              com.kino.backend.grpc.v1.ListTrendingVideosResponse>(
                service, METHODID_LIST_TRENDING_VIDEOS)))
        .addMethod(
          getGetVideoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.kino.backend.grpc.v1.GetVideoRequest,
              com.kino.backend.grpc.v1.Video>(
                service, METHODID_GET_VIDEO)))
        .build();
  }

  private static abstract class CatalogServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    CatalogServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.kino.backend.grpc.v1.KinoProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("CatalogService");
    }
  }

  private static final class CatalogServiceFileDescriptorSupplier
      extends CatalogServiceBaseDescriptorSupplier {
    CatalogServiceFileDescriptorSupplier() {}
  }

  private static final class CatalogServiceMethodDescriptorSupplier
      extends CatalogServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    CatalogServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (CatalogServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new CatalogServiceFileDescriptorSupplier())
              .addMethod(getListTrendingVideosMethod())
              .addMethod(getGetVideoMethod())
              .build();
        }
      }
    }
    return result;
  }
}
