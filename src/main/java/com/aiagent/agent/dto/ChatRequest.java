package com.aiagent.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {

    @NotBlank(message = "Message must not be blank")
    @Size(max = 10000, message = "Message too long")
    private String message;

    private String userId;
}
