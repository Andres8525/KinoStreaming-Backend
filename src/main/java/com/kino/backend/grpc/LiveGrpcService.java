package com.kino.backend.grpc;

import com.kino.backend.grpc.v1.ListLiveStreamsRequest;
import com.kino.backend.grpc.v1.LiveServiceGrpc;
import com.kino.backend.grpc.v1.LiveStreamsSnapshot;
import com.kino.backend.grpc.v1.WatchLiveStreamsRequest;
import com.kino.backend.service.LiveStreamService;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class LiveGrpcService extends LiveServiceGrpc.LiveServiceImplBase {
    static final int MIN_INTERVAL_SECONDS = 1;
    static final int MAX_INTERVAL_SECONDS = 60;
    static final int DEFAULT_INTERVAL_SECONDS = 5;
    static final int DEFAULT_MAX_UPDATES = 60;
    static final int MAX_UPDATES = 720;

    private final LiveStreamService liveStreamService;
    private final Clock clock = Clock.systemUTC();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "kino-grpc-live");
        thread.setDaemon(true);
        return thread;
    });

    public LiveGrpcService(LiveStreamService liveStreamService) {
        this.liveStreamService = liveStreamService;
    }

    @Override
    public void listLiveStreams(ListLiveStreamsRequest request, StreamObserver<LiveStreamsSnapshot> responseObserver) {
        try {
            responseObserver.onNext(snapshot());
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(GrpcErrors.toStatus(exception));
        }
    }

    @Override
    public void watchLiveStreams(WatchLiveStreamsRequest request, StreamObserver<LiveStreamsSnapshot> responseObserver) {
        int interval = request.getIntervalSeconds() == 0 ? DEFAULT_INTERVAL_SECONDS : request.getIntervalSeconds();
        int maxUpdates = request.getMaxUpdates() == 0 ? DEFAULT_MAX_UPDATES : request.getMaxUpdates();
        if (interval < MIN_INTERVAL_SECONDS || interval > MAX_INTERVAL_SECONDS) {
            responseObserver.onError(GrpcErrors.invalidArgument(
                    "interval_seconds debe estar entre " + MIN_INTERVAL_SECONDS + " y " + MAX_INTERVAL_SECONDS));
            return;
        }
        if (maxUpdates < 1 || maxUpdates > MAX_UPDATES) {
            responseObserver.onError(GrpcErrors.invalidArgument("max_updates debe estar entre 1 y " + MAX_UPDATES));
            return;
        }

        ServerCallStreamObserver<LiveStreamsSnapshot> observer =
                (ServerCallStreamObserver<LiveStreamsSnapshot>) responseObserver;
        AtomicInteger sent = new AtomicInteger();
        AtomicReference<ScheduledFuture<?>> task = new AtomicReference<>();
        observer.setOnCancelHandler(() -> cancel(task));
        task.set(scheduler.scheduleAtFixedRate(() -> {
            if (observer.isCancelled()) {
                cancel(task);
                return;
            }
            try {
                observer.onNext(snapshot());
                if (sent.incrementAndGet() >= maxUpdates) {
                    cancel(task);
                    observer.onCompleted();
                }
            } catch (RuntimeException exception) {
                cancel(task);
                observer.onError(GrpcErrors.toStatus(exception));
            }
        }, 0, interval, TimeUnit.SECONDS));
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }

    private LiveStreamsSnapshot snapshot() {
        return GrpcMapper.liveStreams(liveStreamService.listLive(), clock.instant());
    }

    private static void cancel(AtomicReference<ScheduledFuture<?>> task) {
        ScheduledFuture<?> future = task.get();
        if (future != null) {
            future.cancel(false);
        }
    }
}
