package com.aiagent.agent.controller;

import com.aiagent.agent.dto.*;
import com.aiagent.agent.model.AgentSession;
import com.aiagent.agent.model.ConversationMessage;
import com.aiagent.agent.repository.ConversationMessageRepository;
import com.aiagent.agent.service.AgentService;
import com.aiagent.agent.service.MemoryService;
import com.aiagent.agent.tools.ToolRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Tag(name = "AI Agent", description = "AI Agent with long-term memory")
public class AgentController {

    private final AgentService agentService;
    private final MemoryService memoryService;
    private final ConversationMessageRepository messageRepository;
    private final ToolRegistry toolRegistry;

    // ──────────────────────────── SESSIONS ────────────────────────────

    @Operation(summary = "Create new session")
    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> createSession(
            @RequestParam(required = false) String userId) {
        AgentSession session = agentService.createSession(userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toSessionResponse(session, 0));
    }

    @Operation(summary = "List all sessions (optionally filtered by userId)")
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> listSessions(
            @Parameter(description = "Filter by user ID") @RequestParam(required = false) String userId) {
        List<AgentSession> sessions = memoryService.getUserSessions(userId);
        List<SessionResponse> response = sessions.stream()
                .map(s -> toSessionResponse(s, messageRepository.countBySessionId(s.getId())))
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get session by ID")
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable String sessionId) {
        return memoryService.getSession(sessionId)
                .map(s -> ResponseEntity.ok(toSessionResponse(s, messageRepository.countBySessionId(s.getId()))))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete session and all its messages")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        if (memoryService.getSession(sessionId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        memoryService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────── CHAT ────────────────────────────

    @Operation(summary = "Send message to agent in existing session")
    @PostMapping("/sessions/{sessionId}/chat")
    public ResponseEntity<ChatResponse> chat(
            @PathVariable String sessionId,
            @Valid @RequestBody ChatRequest request) {

        if (memoryService.getSession(sessionId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String response = agentService.chat(sessionId, request.getMessage());

        return ResponseEntity.ok(ChatResponse.builder()
                .sessionId(sessionId)
                .message(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @Operation(summary = "Start new session and send first message")
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> startChat(@Valid @RequestBody ChatRequest request) {
        AgentSession session = agentService.createSession(request.getUserId());
        String response = agentService.chat(session.getId(), request.getMessage());

        return ResponseEntity.ok(ChatResponse.builder()
                .sessionId(session.getId())
                .message(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ──────────────────────────── HISTORY ────────────────────────────

    @Operation(summary = "Get all messages in a session")
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(@PathVariable String sessionId) {
        if (memoryService.getSession(sessionId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<MessageResponse> messages = memoryService.getSessionMessages(sessionId)
                .stream()
                .map(this::toMessageResponse)
                .toList();

        return ResponseEntity.ok(messages);
    }

    // ──────────────────────────── INFO ────────────────────────────

    @Operation(summary = "List available tools")
    @GetMapping("/tools")
    public ResponseEntity<Map<String, String>> getTools() {
        Map<String, String> toolsInfo = new java.util.LinkedHashMap<>();
        toolRegistry.getAllTools().forEach((name, tool) ->
                toolsInfo.put(name, tool.getDescription()));
        return ResponseEntity.ok(toolsInfo);
    }

    // ──────────────────────────── MAPPERS ────────────────────────────

    private SessionResponse toSessionResponse(AgentSession session, long messageCount) {
        return SessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .userId(session.getUserId())
                .messageCount(messageCount)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private MessageResponse toMessageResponse(ConversationMessage message) {
        return MessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .toolName(message.getToolName())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
