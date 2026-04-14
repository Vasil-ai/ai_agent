package com.aiagent.agent.tools;

import java.util.Map;

/**
 * Интерфейс для всех инструментов агента.
 * Каждый инструмент регистрируется как Spring-бин и автоматически
 * подхватывается ToolRegistry.
 */
public interface AgentTool {

    /**
     * Уникальное имя инструмента (используется агентом для вызова).
     * Пример: "calculator", "web_search", "datetime"
     */
    String getName();

    /**
     * Описание инструмента для LLM — чем понятнее, тем лучше агент
     * будет понимать когда его использовать.
     */
    String getDescription();

    /**
     * Описание параметров в виде строки (для промпта LLM).
     * Пример: "expression: string — математическое выражение"
     */
    String getParametersDescription();

    /**
     * Выполняет инструмент с переданными параметрами.
     *
     * @param params карта параметров (имя → значение)
     * @return строковый результат выполнения
     */
    String execute(Map<String, String> params);
}
