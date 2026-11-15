package com.kino.backend.grpc;

import com.kino.backend.ai.TrendingInsightsService;
import com.kino.backend.grpc.v1.GetTrendingInsightsRequest;
import com.kino.backend.grpc.v1.InsightsServiceGrpc;
import com.kino.backend.grpc.v1.TrendingInsights;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

@Component
public class InsightsGrpcService extends InsightsServiceGrpc.InsightsServiceImplBase {
    private final TrendingInsightsService trendingInsightsService;

    public InsightsGrpcService(TrendingInsightsService trendingInsightsService) {
        this.trendingInsightsService = trendingInsightsService;
    }

    @Override
    public void getTrendingInsights(GetTrendingInsightsRequest request,
                                    StreamObserver<TrendingInsights> responseObserver) {
        try {
            responseObserver.onNext(GrpcMapper.insights(trendingInsightsService.insights()));
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(GrpcErrors.toStatus(exception));
        }
    }
}
