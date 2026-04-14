package com.aiagent.agent.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatResponse {
    private String sessionId;
    private String message;
    private LocalDateTime timestamp;
}
