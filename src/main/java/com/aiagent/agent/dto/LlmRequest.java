package com.aiagent.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Запрос к OpenAI-совместимому API (llama.cpp server).
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("messages")
    private List<LlmMessage> messages;

    @JsonProperty("temperature")
    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("stream")
    private Boolean stream;

    @JsonProperty("stop")
    private List<String> stop;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LlmMessage {
        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private String content;
    }
}
