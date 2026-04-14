package com.aiagent.agent;

import com.aiagent.agent.tools.CalculatorTool;
import com.aiagent.agent.tools.DateTimeTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "llm.base-url=http://localhost:9999"  // несуществующий, LLM не нужен для unit-тестов
})
class AiAgentApplicationTests {

    @Autowired
    private CalculatorTool calculatorTool;

    @Autowired
    private DateTimeTool dateTimeTool;

    @Test
    void contextLoads() {
    }

    @Test
    void calculatorTool_basicArithmetic() {
        String result = calculatorTool.execute(Map.of("expression", "2847 * 193"));
        assertThat(result).isEqualTo("549471");
    }

    @Test
    void calculatorTool_division() {
        String result = calculatorTool.execute(Map.of("expression", "100 / 4"));
        assertThat(result).isEqualTo("25");
    }

    @Test
    void calculatorTool_missingParam() {
        String result = calculatorTool.execute(Map.of());
        assertThat(result).startsWith("Error:");
    }

    @Test
    void dateTimeTool_returnsCurrentDate() {
        String result = dateTimeTool.execute(Map.of("timezone", "UTC"));
        assertThat(result).contains("UTC");
        assertThat(result).contains("Current date/time");
    }
}
