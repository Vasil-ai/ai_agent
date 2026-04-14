package com.aiagent.agent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Инструмент для поиска в интернете через DuckDuckGo Instant Answer API.
 * Не требует API-ключа. Возвращает краткий ответ и абстракт.
 */
@Slf4j
@Component
public class WebSearchTool implements AgentTool {

    private static final String DUCKDUCKGO_API = "https://api.duckduckgo.com/";
    private final RestClient restClient;

    public WebSearchTool() {
        this.restClient = RestClient.builder()
                .baseUrl(DUCKDUCKGO_API)
                .defaultHeader("User-Agent", "AI-Agent/1.0")
                .build();
    }

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        return "Searches the internet using DuckDuckGo. Use for finding current information, facts, definitions.";
    }

    @Override
    public String getParametersDescription() {
        return "query: string — the search query to look up";
    }

    @Override
    public String execute(Map<String, String> params) {
        String query = params.get("query");
        if (query == null || query.isBlank()) {
            return "Error: 'query' parameter is required";
        }

        log.debug("Web search: {}", query);

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String response = restClient.get()
                    .uri("?q={query}&format=json&no_html=1&skip_disambig=1", encodedQuery)
                    .retrieve()
                    .body(String.class);

            return parseResponse(response, query);
        } catch (Exception e) {
            log.warn("Web search failed for '{}': {}", query, e.getMessage());
            return "Search failed: " + e.getMessage() + ". Try rephrasing the query.";
        }
    }

    private String parseResponse(String json, String query) {
        if (json == null || json.isBlank()) {
            return "No results found for: " + query;
        }

        StringBuilder result = new StringBuilder();

        String abstractText = extractJsonField(json, "Abstract");
        String answer = extractJsonField(json, "Answer");
        String definition = extractJsonField(json, "Definition");

        if (answer != null && !answer.isBlank()) {
            result.append("Answer: ").append(answer).append("\n");
        }
        if (abstractText != null && !abstractText.isBlank()) {
            result.append("Summary: ").append(abstractText).append("\n");
        }
        if (definition != null && !definition.isBlank()) {
            result.append("Definition: ").append(definition).append("\n");
        }

        if (result.isEmpty()) {
            return "No direct answer found for '" + query + "'. Consider rephrasing or using a more specific query.";
        }

        return result.toString().trim();
    }

    private String extractJsonField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String value = matcher.group(1)
                    .replace("\\n", " ")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .trim();
            return value.isBlank() ? null : value;
        }
        return null;
    }
}
