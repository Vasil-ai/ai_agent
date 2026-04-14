package com.aiagent.agent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Реестр всех доступных инструментов агента.
 * Spring автоматически инжектирует все бины, реализующие AgentTool.
 */
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, AgentTool> tools;

    public ToolRegistry(List<AgentTool> toolList) {
        this.tools = toolList.stream()
                .collect(Collectors.toMap(AgentTool::getName, Function.identity()));
        log.info("Registered {} agent tools: {}", tools.size(), tools.keySet());
    }

    public Optional<AgentTool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Map<String, AgentTool> getAllTools() {
        return tools;
    }

    /**
     * Генерирует описание всех инструментов для системного промпта агента.
     */
    public String buildToolsDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available tools:\n\n");
        tools.forEach((name, tool) -> {
            sb.append("Tool: ").append(name).append("\n");
            sb.append("Description: ").append(tool.getDescription()).append("\n");
            sb.append("Parameters: ").append(tool.getParametersDescription()).append("\n\n");
        });
        return sb.toString();
    }
}
