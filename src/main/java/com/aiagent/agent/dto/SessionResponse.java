package com.aiagent.agent.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionResponse {
    private String id;
    private String title;
    private String userId;
    private long messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
