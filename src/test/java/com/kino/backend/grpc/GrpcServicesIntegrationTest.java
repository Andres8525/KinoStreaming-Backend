package com.kino.backend.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kino.backend.dto.CreateVideoRequest;
import com.kino.backend.grpc.v1.CatalogServiceGrpc;
import com.kino.backend.grpc.v1.GetTrendingInsightsRequest;
import com.kino.backend.grpc.v1.GetVideoRequest;
import com.kino.backend.grpc.v1.InsightsServiceGrpc;
import com.kino.backend.grpc.v1.ListLiveStreamsRequest;
import com.kino.backend.grpc.v1.ListTrendingVideosRequest;
import com.kino.backend.grpc.v1.LiveServiceGrpc;
import com.kino.backend.grpc.v1.LiveStreamsSnapshot;
import com.kino.backend.grpc.v1.WatchLiveStreamsRequest;
import com.kino.backend.model.User;
import com.kino.backend.model.UserRole;
import com.kino.backend.repository.UserRepository;
import com.kino.backend.service.VideoService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GrpcServicesIntegrationTest {
    @Autowired
    private CatalogGrpcService catalog;
    @Autowired
    private InsightsGrpcService insights;
    @Autowired
    private LiveGrpcService live;
    @Autowired
    private VideoService videoService;
    @Autowired
    private UserRepository userRepository;

    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    void startInProcessServer() throws Exception {
        String name = InProcessServerBuilder.generateName();
        LoggingServerInterceptor logging = new LoggingServerInterceptor();
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(ServerInterceptors.intercept(catalog, logging))
                .addService(ServerInterceptors.intercept(insights, logging))
                .addService(ServerInterceptors.intercept(live, logging))
                .build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
    }

    @AfterEach
    void stopInProcessServer() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void listsTrendingVideosAndFiltersByCategory() {
        String email = "grpc-" + UUID.randomUUID() + "@example.com";
        userRepository.save(new User("Grpc Creator", email, "hash", UserRole.CREATOR));
        UUID documentary = videoService.create(email, video("Documental gRPC", "Documental")).id();
        videoService.create(email, video("Música gRPC", "Música"));

        var stub = CatalogServiceGrpc.newBlockingStub(channel).withDeadlineAfter(5, TimeUnit.SECONDS);
        var filtered = stub.listTrendingVideos(ListTrendingVideosRequest.newBuilder().setCategory("documental").build());

        assertThat(filtered.getVideosList()).extracting(com.kino.backend.grpc.v1.Video::getTitle)
                .contains("Documental gRPC").doesNotContain("Música gRPC");
        var found = stub.getVideo(GetVideoRequest.newBuilder().setId(documentary.toString()).build());
        assertThat(found.getCategory()).isEqualTo("Documental");
        assertThat(found.getCreatorName()).isEqualTo("Grpc Creator");
    }

    @Test
    void mapsErrorsToGrpcStatusCodes() {
        var stub = CatalogServiceGrpc.newBlockingStub(channel);

        assertStatus(() -> stub.getVideo(GetVideoRequest.newBuilder().setId("not-a-uuid").build()),
                Status.Code.INVALID_ARGUMENT);
        assertStatus(() -> stub.getVideo(GetVideoRequest.newBuilder().setId(UUID.randomUUID().toString()).build()),
                Status.Code.NOT_FOUND);
        assertStatus(() -> stub.listTrendingVideos(ListTrendingVideosRequest.newBuilder().setLimit(500).build()),
                Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void returnsTrendingInsights() {
        var response = InsightsServiceGrpc.newBlockingStub(channel)
                .getTrendingInsights(GetTrendingInsightsRequest.getDefaultInstance());

        assertThat(response.getSource()).isEqualTo("kino-analytics");
        assertThat(response.getSummary()).isNotBlank();
    }

    @Test
    void streamsLiveSnapshotsUntilTheRequestedLimit() {
        var stub = LiveServiceGrpc.newBlockingStub(channel).withDeadlineAfter(10, TimeUnit.SECONDS);
        assertThat(stub.listLiveStreams(ListLiveStreamsRequest.getDefaultInstance()).getGeneratedAt()).isNotBlank();

        var updates = stub.watchLiveStreams(WatchLiveStreamsRequest.newBuilder()
                .setIntervalSeconds(1).setMaxUpdates(2).build());
        List<LiveStreamsSnapshot> received = new java.util.ArrayList<>();
        updates.forEachRemaining(received::add);

        assertThat(received).hasSize(2);
        assertStatus(() -> stub.watchLiveStreams(WatchLiveStreamsRequest.newBuilder().setIntervalSeconds(600).build())
                .hasNext(), Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void nettyServerReportsHealthAndShutsDownGracefully() throws Exception {
        GrpcServerLifecycle lifecycle = new GrpcServerLifecycle(
                new GrpcProperties(true, 0, 0, Duration.ofSeconds(2)), catalog, insights, live);
        lifecycle.start();
        ManagedChannel netty = NettyChannelBuilder.forAddress("localhost", lifecycle.port()).usePlaintext().build();
        try {
            HealthCheckResponse health = HealthGrpc.newBlockingStub(netty).withDeadlineAfter(5, TimeUnit.SECONDS)
                    .check(HealthCheckRequest.newBuilder().setService("kino.v1.CatalogService").build());
            assertThat(health.getStatus()).isEqualTo(HealthCheckResponse.ServingStatus.SERVING);
        } finally {
            netty.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            lifecycle.stop();
        }
        assertThat(lifecycle.isRunning()).isFalse();
    }

    private static CreateVideoRequest video(String title, String category) {
        return new CreateVideoRequest(title, "Descripción", "https://cdn.example.com/video.mp4", null,
                List.of(category, "Indie"), BigDecimal.ZERO, null);
    }

    private static void assertStatus(Runnable call, Status.Code code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(StatusRuntimeException.class,
                exception -> assertThat(exception.getStatus().getCode()).isEqualTo(code));
    }
}
