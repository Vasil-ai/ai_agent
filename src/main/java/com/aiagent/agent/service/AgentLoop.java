package com.aiagent.agent.service;

import com.aiagent.agent.config.LlmConfig;
import com.aiagent.agent.dto.LlmRequest;
import com.aiagent.agent.tools.AgentTool;
import com.aiagent.agent.tools.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Реализация паттерна ReAct (Reason + Act).
 *
 * Цикл:
 *  1. Собираем контекст (системный промпт + история + новый вопрос)
 *  2. Спрашиваем LLM — она либо вызывает инструмент, либо даёт финальный ответ
 *  3. Если вызван инструмент — выполняем его, добавляем результат в контекст и повторяем
 *  4. Если финальный ответ — возвращаем его пользователю
 *
 * Формат вызова инструмента (который LLM должна соблюдать):
 *   TOOL_CALL: tool_name
 *   PARAMS: {"key": "value"}
 *
 * Финальный ответ:
 *   FINAL_ANSWER: текст ответа
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentLoop {

    // Ищем имя инструмента — в любом из форматов которые может вернуть модель
    private static final Pattern TOOL_NAME_PATTERN = Pattern.compile(
            "(?:TOOL_CALL:|TOOL:|=== TOOL[_: ]+)\\s*:?\\s*([\\w]+)",
            Pattern.CASE_INSENSITIVE);

    // Ищем JSON с параметрами
    private static final Pattern PARAMS_PATTERN = Pattern.compile(
            "PARAMS:\\s*(\\{[\\s\\S]*?\\})",
            Pattern.CASE_INSENSITIVE);

    // Поддерживаем форматы: "FINAL_ANSWER: text" и "=== FINAL_ANSWER ===\ntext"
    private static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile(
            "(?:===\\s*FINAL_ANSWER\\s*===\\s*\\n|FINAL_ANSWER:\\s*)(.+)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final LlmConfig llmConfig;

    /**
     * Запускает агентный цикл.
     *
     * @param userMessage   текущее сообщение пользователя
     * @param sessionHistory история сообщений текущей сессии (роль → контент)
     * @return финальный ответ агента
     */
    public String run(String userMessage, List<Map.Entry<String, String>> sessionHistory) {
        List<LlmRequest.LlmMessage> messages = buildInitialMessages(userMessage, sessionHistory);

        for (int iteration = 0; iteration < llmConfig.getMaxIterations(); iteration++) {
            log.info("AgentLoop iteration {}/{}", iteration + 1, llmConfig.getMaxIterations());

            String llmResponse = llmClient.chat(messages);
            log.debug("LLM raw response:\n{}", llmResponse);

            messages.add(LlmRequest.LlmMessage.builder()
                    .role("assistant")
                    .content(llmResponse)
                    .build());

            // Проверяем финальный ответ
            Matcher finalMatcher = FINAL_ANSWER_PATTERN.matcher(llmResponse);
            if (finalMatcher.find()) {
                String answer = finalMatcher.group(1).trim();
                log.info("Agent finished in {} iteration(s)", iteration + 1);
                return answer;
            }

            // Проверяем вызов инструмента — ищем имя и параметры независимо
            Matcher toolNameMatcher = TOOL_NAME_PATTERN.matcher(llmResponse);
            Matcher paramsMatcher = PARAMS_PATTERN.matcher(llmResponse);

            if (toolNameMatcher.find()) {
                String toolName = toolNameMatcher.group(1).trim().toLowerCase();
                String paramsJson = paramsMatcher.find() ? paramsMatcher.group(1).trim() : "{}";

                log.info("Detected tool call: {} with params: {}", toolName, paramsJson);
                String toolResult = executeTool(toolName, paramsJson);
                log.info("Tool '{}' result: {}", toolName, toolResult);

                messages.add(LlmRequest.LlmMessage.builder()
                        .role("user")
                        .content("TOOL_RESULT for " + toolName + ":\n" + toolResult
                                + "\n\nNow provide your FINAL_ANSWER based on this result.")
                        .build());
            } else {
                // LLM не следует формату — трактуем ответ как финальный
                log.warn("LLM response doesn't match any known format, treating as final answer");
                return llmResponse.trim();
            }
        }

        return "I was unable to complete the task within the allowed number of steps. Please try rephrasing your request.";
    }

    private List<LlmRequest.LlmMessage> buildInitialMessages(
            String userMessage,
            List<Map.Entry<String, String>> sessionHistory) {

        List<LlmRequest.LlmMessage> messages = new ArrayList<>();

        // Системный промпт с инструкциями ReAct (текущая дата уже включена)
        messages.add(LlmRequest.LlmMessage.builder()
                .role("system")
                .content(buildSystemPrompt())
                .build());

        // История предыдущих сообщений сессии (долгосрочная память)
        for (Map.Entry<String, String> entry : sessionHistory) {
            messages.add(LlmRequest.LlmMessage.builder()
                    .role(entry.getKey())
                    .content(entry.getValue())
                    .build());
        }

        // Проверяем нужна ли предзагрузка инструментов для этого запроса
        String preloadedContext = preloadToolContextIfNeeded(userMessage);

        String fullUserMessage = preloadedContext.isBlank()
                ? userMessage
                : userMessage + "\n\n[Context from tools: " + preloadedContext + "]";

        messages.add(LlmRequest.LlmMessage.builder()
                .role("user")
                .content(fullUserMessage)
                .build());

        return messages;
    }

    /**
     * Для вопросов о дате/времени автоматически инжектируем реальный результат
     * datetime-инструмента, не полагаясь на то что модель сама его вызовет.
     */
    private String preloadToolContextIfNeeded(String message) {
        String lower = message.toLowerCase();
        if (lower.matches(".*\\b(today|date|time|day|week|month|year|now|current)\\b.*")) {
            Optional<AgentTool> datetimeTool = toolRegistry.getTool("datetime");
            if (datetimeTool.isPresent()) {
                String result = datetimeTool.get().execute(Map.of("timezone", "Europe/Moscow"));
                log.debug("Pre-loaded datetime context: {}", result);
                return result;
            }
        }
        return "";
    }

    private String buildSystemPrompt() {
        String currentDateTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy HH:mm:ss"));

        return """
                You are a helpful AI assistant. You have access to tools to help answer questions accurately.
                
                CURRENT DATE AND TIME: %s
                
                ALWAYS respond using EXACTLY one of these two formats:
                
                === FORMAT 1: Use a tool ===
                TOOL: tool_name
                PARAMS: {"param": "value"}
                
                === FORMAT 2: Give final answer ===
                FINAL_ANSWER: your answer here
                
                === RULES ===
                - For ANY math/arithmetic → ALWAYS use calculator tool (do not calculate in your head)
                - For internet/current events → use web_search tool
                - For general knowledge → use FINAL_ANSWER directly
                - After getting TOOL_RESULT → respond with FINAL_ANSWER
                
                === EXAMPLES ===
                User: What is 15 * 7?
                TOOL: calculator
                PARAMS: {"expression": "15 * 7"}
                
                [TOOL_RESULT: 105]
                FINAL_ANSWER: 15 × 7 = 105
                
                User: What is the capital of France?
                FINAL_ANSWER: The capital of France is Paris.
                
                """.formatted(currentDateTime) + toolRegistry.buildToolsDescription();
    }

    private String executeTool(String toolName, String paramsJson) {
        Optional<AgentTool> toolOpt = toolRegistry.getTool(toolName);

        if (toolOpt.isEmpty()) {
            return "Error: Tool '" + toolName + "' not found. Available tools: " +
                    String.join(", ", toolRegistry.getAllTools().keySet());
        }

        Map<String, String> params = parseParams(paramsJson);
        try {
            return toolOpt.get().execute(params);
        } catch (Exception e) {
            log.error("Tool '{}' execution failed: {}", toolName, e.getMessage());
            return "Error executing tool '" + toolName + "': " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseParams(String paramsJson) {
        Map<String, String> result = new HashMap<>();
        if (paramsJson == null || paramsJson.isBlank()) {
            return result;
        }

        // Простой парсер JSON-объекта с парами ключ-значение
        Pattern pairPattern = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pairPattern.matcher(paramsJson);
        while (matcher.find()) {
            result.put(matcher.group(1), matcher.group(2));
        }
        return result;
    }
}
