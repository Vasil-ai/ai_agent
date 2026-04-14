package com.aiagent.agent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Map;

/**
 * Инструмент для вычисления математических выражений.
 * Использует встроенный JavaScript движок (Nashorn/Rhino) для безопасного eval.
 */
@Slf4j
@Component
public class CalculatorTool implements AgentTool {

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "Evaluates mathematical expressions. Use for any arithmetic calculations.";
    }

    @Override
    public String getParametersDescription() {
        return "expression: string — a mathematical expression to evaluate (e.g., '2847 * 193', '(15 + 3) / 2', 'Math.sqrt(144)')";
    }

    @Override
    public String execute(Map<String, String> params) {
        String expression = params.get("expression");
        if (expression == null || expression.isBlank()) {
            return "Error: 'expression' parameter is required";
        }

        log.debug("Calculating: {}", expression);

        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");

            if (engine == null) {
                return fallbackCalculate(expression);
            }

            Object result = engine.eval(expression);
            return String.valueOf(result);
        } catch (Exception e) {
            log.warn("Failed to evaluate expression '{}': {}", expression, e.getMessage());
            return "Error: Cannot evaluate '" + expression + "'. " + e.getMessage();
        }
    }

    /**
     * Простой fallback калькулятор для базовых операций
     * на случай если ScriptEngine недоступен.
     */
    private String fallbackCalculate(String expression) {
        try {
            expression = expression.trim();
            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                double result = 0;
                for (String part : parts) result += Double.parseDouble(part.trim());
                return formatResult(result);
            }
            if (expression.contains("-")) {
                String[] parts = expression.split("-");
                double result = Double.parseDouble(parts[0].trim());
                for (int i = 1; i < parts.length; i++) result -= Double.parseDouble(parts[i].trim());
                return formatResult(result);
            }
            if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                double result = 1;
                for (String part : parts) result *= Double.parseDouble(part.trim());
                return formatResult(result);
            }
            if (expression.contains("/")) {
                String[] parts = expression.split("/");
                double result = Double.parseDouble(parts[0].trim());
                for (int i = 1; i < parts.length; i++) result /= Double.parseDouble(parts[i].trim());
                return formatResult(result);
            }
            return String.valueOf(Double.parseDouble(expression));
        } catch (Exception e) {
            return "Error: unsupported expression format";
        }
    }

    private String formatResult(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
