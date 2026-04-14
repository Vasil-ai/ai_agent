package com.aiagent.agent.tools;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Инструмент для получения текущей даты и времени.
 */
@Component
public class DateTimeTool implements AgentTool {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getName() {
        return "datetime";
    }

    @Override
    public String getDescription() {
        return "Returns the current date and time. Use when you need to know today's date, current time, or day of week.";
    }

    @Override
    public String getParametersDescription() {
        return "timezone: string (optional) — timezone ID, e.g. 'Europe/Moscow', 'UTC'. Default: system timezone.";
    }

    @Override
    public String execute(Map<String, String> params) {
        String timezone = params.getOrDefault("timezone", ZoneId.systemDefault().getId());

        try {
            ZoneId zoneId = ZoneId.of(timezone);
            LocalDateTime now = LocalDateTime.now(zoneId);
            return String.format("Current date/time in %s: %s (%s)",
                    timezone,
                    now.format(FORMATTER),
                    now.getDayOfWeek().toString());
        } catch (Exception e) {
            LocalDateTime now = LocalDateTime.now();
            return String.format("Current date/time: %s (%s)",
                    now.format(FORMATTER),
                    now.getDayOfWeek().toString());
        }
    }
}
