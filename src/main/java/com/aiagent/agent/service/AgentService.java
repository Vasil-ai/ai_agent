package com.aiagent.agent.service;

import com.aiagent.agent.model.AgentSession;
import com.aiagent.agent.model.ConversationMessage.MessageRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Главный сервис агента — координирует LLM, инструменты и память.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentLoop agentLoop;
    private final MemoryService memoryService;

    /**
     * Создаёт новую сессию и возвращает её ID.
     */
    public AgentSession createSession(String userId) {
        return memoryService.createSession(userId, "New conversation");
    }

    /**
     * Обрабатывает сообщение пользователя в рамках существующей сессии.
     *
     * @param sessionId  ID сессии
     * @param userMessage сообщение пользователя
     * @return ответ агента
     */
    public String chat(String sessionId, String userMessage) {
        // Проверяем что сессия существует
        memoryService.getSession(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        log.info("Processing message in session {}: '{}'",
                sessionId, userMessage.length() > 50 ? userMessage.substring(0, 50) + "..." : userMessage);

        // Сохраняем сообщение пользователя
        memoryService.saveMessage(sessionId, MessageRole.USER, userMessage);

        // Получаем историю для контекста LLM
        List<Map.Entry<String, String>> history = memoryService.getSessionHistoryForLlm(sessionId);
        // Исключаем последнее сообщение (только что добавленное) — оно передаётся отдельно
        List<Map.Entry<String, String>> previousHistory = history.isEmpty()
                ? List.of()
                : history.subList(0, history.size() - 1);

        // Запускаем агентный цикл
        String agentResponse = agentLoop.run(userMessage, previousHistory);

        // Сохраняем ответ агента
        memoryService.saveMessage(sessionId, MessageRole.ASSISTANT, agentResponse);

        // Обновляем заголовок сессии по первому сообщению
        updateSessionTitleIfNeeded(sessionId, userMessage);

        log.info("Agent response for session {}: {} chars", sessionId, agentResponse.length());
        return agentResponse;
    }

    private void updateSessionTitleIfNeeded(String sessionId, String userMessage) {
        memoryService.getSession(sessionId).ifPresent(session -> {
            if ("New conversation".equals(session.getTitle())) {
                String title = userMessage.length() > 60
                        ? userMessage.substring(0, 60) + "..."
                        : userMessage;
                memoryService.updateSessionTitle(sessionId, title);
            }
        });
    }
}
