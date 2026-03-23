package com.cjw;

import com.cjw.model.optimization.CodeOptimizationRequest;
import com.cjw.model.optimization.CodeOptimizationResponse;
import com.cjw.service.optimization.CodeOptimizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CollaborativeJavaWorkspaceBackendApplicationTests {

    @Autowired
    private CodeOptimizationService optimizationService;

    @Test
    void contextLoads() {
        assertThat(optimizationService).isNotNull();
    }

    @Test
    void testCodeOptimization() throws Exception {
        // Sample Java code with issues
        String testCode = """
            public class TestClass {
                public void badMethod() {
                    String result = "";
                    for (int i = 0; i < 10; i++) {
                        result = result + "item" + i; // String concatenation in loop
                    }
                    System.out.println(result);
                }

                private void unusedMethod() {
                    // This method is never called
                }
            }
            """;

        CodeOptimizationRequest request = new CodeOptimizationRequest(
            testCode,
            "java",
            Arrays.asList("performance", "readability")
        );

        CompletableFuture<CodeOptimizationResponse> future = optimizationService.optimizeCode(request);
        CodeOptimizationResponse response = future.get(30, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.getIssues()).isNotNull();
        assertThat(response.getSuggestions()).isNotNull();
        assertThat(response.getMetrics()).isNotNull();

        // Should detect the string concatenation issue
        boolean hasStringBuilderSuggestion = response.getSuggestions().stream()
            .anyMatch(s -> s.getTitle().contains("StringBuilder"));
        assertThat(hasStringBuilderSuggestion).isTrue();
    }
}