package com.aiagent.agent.service;

import com.aiagent.agent.config.LlmConfig;
import com.aiagent.agent.dto.LlmRequest;
import com.aiagent.agent.dto.LlmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Клиент для общения с llama.cpp сервером через OpenAI-совместимый API.
 * llama.cpp запускается командой:
 *   ./llama-server -m Ministral-3-3B-Instruct.gguf --port 8080 -c 4096
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClient {

    private final RestClient llmRestClient;
    private final LlmConfig llmConfig;

    /**
     * Отправляет список сообщений в LLM и возвращает текстовый ответ.
     */
    public String chat(List<LlmRequest.LlmMessage> messages) {
        LlmRequest request = LlmRequest.builder()
                .model(llmConfig.getModel())
                .messages(messages)
                .temperature(llmConfig.getTemperature())
                .maxTokens(llmConfig.getMaxTokens())
                .stream(false)
                .build();

        log.debug("Sending {} messages to LLM", messages.size());

        try {
            LlmResponse response = llmRestClient.post()
                    .uri("/v1/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(LlmResponse.class);

            if (response == null) {
                throw new RuntimeException("Empty response from LLM");
            }

            String content = response.getFirstChoiceContent();
            log.debug("LLM response: {} chars, finish_reason={}",
                    content.length(),
                    response.getChoices().isEmpty() ? "none" : response.getChoices().get(0).getFinishReason());

            return content;

        } catch (RestClientException e) {
            log.error("Failed to call LLM at {}: {}", llmConfig.getBaseUrl(), e.getMessage());
            throw new RuntimeException("LLM service unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * Простой single-turn запрос к LLM (без истории).
     */
    public String complete(String systemPrompt, String userMessage) {
        List<LlmRequest.LlmMessage> messages = List.of(
                LlmRequest.LlmMessage.builder().role("system").content(systemPrompt).build(),
                LlmRequest.LlmMessage.builder().role("user").content(userMessage).build()
        );
        return chat(messages);
    }
}
