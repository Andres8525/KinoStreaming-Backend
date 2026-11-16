package com.kino.backend.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kino.backend.ai.KinoAssistant;
import com.kino.backend.dto.AiChatResponse;
import com.kino.backend.dto.ChatMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AiWorkerTest {
    private static final List<ChatMessage> QUESTION = List.of(ChatMessage.user("¿Qué veo?"));

    private final KinoAssistant assistant = mock(KinoAssistant.class);
    private final CountDownLatch release = new CountDownLatch(1);
    private AiWorker worker;

    @AfterEach
    void tearDown() throws InterruptedException {
        release.countDown();
        if (worker != null) {
            worker.destroy();
        }
    }

    @Test
    void answersOnAWorkerThread() throws Exception {
        when(assistant.prepare(anyList())).thenReturn(QUESTION);
        when(assistant.reply(anyList())).thenAnswer(invocation ->
                response(Thread.currentThread().getName()));
        worker = new AiWorker(assistant, 1, 5, Duration.ofSeconds(5));

        AiChatResponse response = worker.chat(QUESTION).get(5, TimeUnit.SECONDS);

        assertThat(response.reply()).startsWith("kino-ai-worker-");
        assertThat(worker.status()).containsEntry("threads", 1);
    }

    @Test
    void answersWithTheBuiltInAssistantWhenTheAiIsTooSlow() throws Exception {
        when(assistant.prepare(anyList())).thenReturn(QUESTION);
        when(assistant.reply(anyList())).thenAnswer(invocation -> {
            release.await(5, TimeUnit.SECONDS);
            return response("tarde");
        });
        when(assistant.builtInReply(anyList())).thenReturn(response("respuesta de Kino"));
        worker = new AiWorker(assistant, 1, 5, Duration.ofMillis(100));

        assertThat(worker.chat(QUESTION).get(5, TimeUnit.SECONDS).reply()).isEqualTo("respuesta de Kino");
    }

    @Test
    void rejectsQuestionsWhenTheQueueIsFull() {
        when(assistant.prepare(anyList())).thenReturn(QUESTION);
        when(assistant.reply(anyList())).thenAnswer(invocation -> {
            release.await(5, TimeUnit.SECONDS);
            return response("ok");
        });
        worker = new AiWorker(assistant, 1, 1, Duration.ofSeconds(5));

        worker.chat(QUESTION);
        worker.chat(QUESTION);

        assertThatThrownBy(() -> worker.chat(QUESTION)).isInstanceOf(RejectedExecutionException.class);
    }

    private static AiChatResponse response(String reply) {
        return new AiChatResponse(reply, "kino-analytics", List.of(), Instant.now());
    }
}
