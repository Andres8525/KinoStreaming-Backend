package com.kino.backend.ai;

import com.kino.backend.dto.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class AiChatGateway {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatGateway.class);

    private final ObjectProvider<AiTextGenerator> generators;
    private final AiProperties properties;

    public AiChatGateway(ObjectProvider<AiTextGenerator> generators, AiProperties properties) {
        this.generators = generators;
        this.properties = properties;
    }

    public Optional<String> chat(String instructions, List<ChatMessage> conversation) {
        if (!properties.enabled()) {
            return Optional.empty();
        }
        AiTextGenerator generator = generators.getIfAvailable();
        if (generator == null) {
            return Optional.empty();
        }
        try {
            return generator.chat(instructions, conversation).map(String::trim).filter(text -> !text.isEmpty());
        } catch (RuntimeException exception) {
            LOGGER.warn("AI provider '{}' failed to answer; using the built-in answer: {}", generator.name(),
                    exception.getMessage());
            return Optional.empty();
        }
    }
}
