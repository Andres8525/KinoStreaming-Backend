package com.kino.backend.worker;

import com.kino.backend.ai.KinoAssistant;
import com.kino.backend.dto.AiChatResponse;
import com.kino.backend.dto.ChatMessage;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiWorker implements DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiWorker.class);

    private final KinoAssistant assistant;
    private final ThreadPoolExecutor executor;
    private final Duration timeout;

    public AiWorker(KinoAssistant assistant,
                    @Value("${kino.workers.ai.threads:2}") int threads,
                    @Value("${kino.workers.ai.queue-capacity:50}") int queueCapacity,
                    @Value("${kino.workers.ai.timeout:PT25S}") Duration timeout) {
        this.assistant = assistant;
        this.timeout = timeout;
        int size = Math.max(1, threads);
        this.executor = new ThreadPoolExecutor(size, size, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(Math.max(1, queueCapacity)), threadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        this.executor.allowCoreThreadTimeOut(true);
    }

    public CompletableFuture<AiChatResponse> chat(List<ChatMessage> conversation) {
        List<ChatMessage> history = assistant.prepare(conversation);
        return CompletableFuture.supplyAsync(() -> assistant.reply(history), executor)
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(error -> {
                    Throwable cause = error instanceof CompletionException && error.getCause() != null
                            ? error.getCause() : error;
                    if (cause instanceof TimeoutException) {
                        LOGGER.warn("Kino IA took longer than {}; answering with the built-in assistant", timeout);
                        return assistant.builtInReply(history);
                    }
                    throw cause instanceof RuntimeException runtime ? runtime : new CompletionException(cause);
                });
    }

    public <T> CompletableFuture<T> run(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("threads", executor.getMaximumPoolSize());
        status.put("active", executor.getActiveCount());
        status.put("queued", executor.getQueue().size());
        status.put("completed", executor.getCompletedTaskCount());
        return status;
    }

    @Override
    public void destroy() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }

    private static ThreadFactory threadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "kino-ai-worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
