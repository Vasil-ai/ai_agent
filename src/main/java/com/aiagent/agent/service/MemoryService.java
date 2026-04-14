package com.aiagent.agent.service;

import com.aiagent.agent.model.AgentSession;
import com.aiagent.agent.model.ConversationMessage;
import com.aiagent.agent.model.ConversationMessage.MessageRole;
import com.aiagent.agent.repository.AgentSessionRepository;
import com.aiagent.agent.repository.ConversationMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис управления долгосрочной памятью агента.
 * Хранит сессии и всю историю сообщений в БД.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    private final AgentSessionRepository sessionRepository;
    private final ConversationMessageRepository messageRepository;

    @Transactional
    public AgentSession createSession(String userId, String title) {
        AgentSession session = new AgentSession();
        session.setUserId(userId);
        session.setTitle(title != null ? title : "New conversation");
        AgentSession saved = sessionRepository.save(session);
        log.info("Created session {} for user {}", saved.getId(), userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<AgentSession> getSession(String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    @Transactional(readOnly = true)
    public List<AgentSession> getUserSessions(String userId) {
        if (userId != null && !userId.isBlank()) {
            return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        }
        return sessionRepository.findAllOrderByUpdatedAtDesc();
    }

    @Transactional
    public ConversationMessage saveMessage(String sessionId, MessageRole role, String content) {
        AgentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        ConversationMessage message = new ConversationMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);

        ConversationMessage saved = messageRepository.save(message);
        log.debug("Saved message [{}] for session {}", role, sessionId);
        return saved;
    }

    @Transactional
    public ConversationMessage saveToolMessage(String sessionId, MessageRole role,
                                                String content, String toolName) {
        AgentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        ConversationMessage message = new ConversationMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        message.setToolName(toolName);

        return messageRepository.save(message);
    }

    /**
     * Возвращает историю сессии в формате, подходящем для LLM (роль → контент).
     * Возвращает только USER и ASSISTANT сообщения (без системных и tool-вызовов).
     */
    @Transactional(readOnly = true)
    public List<Map.Entry<String, String>> getSessionHistoryForLlm(String sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .filter(m -> m.getRole() == MessageRole.USER || m.getRole() == MessageRole.ASSISTANT)
                .map(m -> (Map.Entry<String, String>) new AbstractMap.SimpleEntry<>(
                        m.getRole().name().toLowerCase(),
                        m.getContent()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> getSessionMessages(String sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public void updateSessionTitle(String sessionId, String title) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setTitle(title);
            sessionRepository.save(session);
        });
    }

    @Transactional
    public void deleteSession(String sessionId) {
        sessionRepository.deleteById(sessionId);
        log.info("Deleted session {}", sessionId);
    }
}
