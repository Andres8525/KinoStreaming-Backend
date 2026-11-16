package com.kino.backend.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kino.backend.ai.TrendingInsightsService;
import com.kino.backend.dto.TrendingInsightsResponse;
import com.kino.backend.realtime.KinoRealtimeHub;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InsightsRefreshWorkerTest {
    private final TrendingInsightsService insightsService = mock(TrendingInsightsService.class);
    private final KinoRealtimeHub hub = mock(KinoRealtimeHub.class);

    @Test
    void doesNothingWhileNobodyIsConnected() {
        when(hub.connectionCount()).thenReturn(0);

        new InsightsRefreshWorker(insightsService, hub, true).refresh();

        verify(insightsService, never()).refreshStatistics();
    }

    @Test
    void broadcastsOnlyWhenTheStatisticsChange() {
        when(hub.connectionCount()).thenReturn(2);
        when(insightsService.refreshStatistics()).thenReturn(insights(100), insights(100), insights(140));
        InsightsRefreshWorker worker = new InsightsRefreshWorker(insightsService, hub, true);

        worker.refresh();
        worker.refresh();
        worker.refresh();

        verify(hub, times(2)).broadcast(eq("insights"), any());
    }

    @Test
    void canBeDisabled() {
        when(hub.connectionCount()).thenReturn(3);

        new InsightsRefreshWorker(insightsService, hub, false).refresh();

        verify(insightsService, never()).refreshStatistics();
    }

    @Test
    void keepsRunningWhenTheDatabaseFails() {
        when(hub.connectionCount()).thenReturn(1);
        when(insightsService.refreshStatistics()).thenThrow(new IllegalStateException("db down"));

        new InsightsRefreshWorker(insightsService, hub, true).refresh();

        verify(hub, never()).broadcast(any(), any());
    }

    private static TrendingInsightsResponse insights(long totalViews) {
        return new TrendingInsightsResponse(Instant.now(), "kino-analytics", "Resumen", List.of(), List.of(), List.of(),
                totalViews, 0);
    }
}
