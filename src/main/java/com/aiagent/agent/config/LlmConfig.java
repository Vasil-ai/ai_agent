package com.aiagent.agent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@ConfigurationProperties(prefix = "llm")
@Getter
@Setter
public class LlmConfig {

    private String baseUrl = "http://localhost:8080";
    private String model = "ministral-3b";
    private double temperature = 0.7;
    private int maxTokens = 2048;
    private int maxIterations = 10;
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 120;

    @Bean
    public RestClient llmRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
