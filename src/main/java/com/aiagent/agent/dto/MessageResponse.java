package com.aiagent.agent.dto;

import com.aiagent.agent.model.ConversationMessage.MessageRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MessageResponse {
    private Long id;
    private MessageRole role;
    private String content;
    private String toolName;
    private LocalDateTime createdAt;
}
