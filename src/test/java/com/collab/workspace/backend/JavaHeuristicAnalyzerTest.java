package com.collab.workspace.backend;

import com.collab.workspace.backend.dto.JavaWorkspaceRequest;
import com.collab.workspace.backend.engine.JavaHeuristicAnalyzer;
import com.collab.workspace.backend.model.AnalysisResult;
import com.collab.workspace.backend.model.OptimizationResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaHeuristicAnalyzerTest {

    @Test
    void shouldGenerateIssuesAndMetrics() {
        JavaWorkspaceRequest request = new JavaWorkspaceRequest();
        request.setWorkspaceName("demo");
        request.setEntryFile("src/Test.java");
        request.setFiles(Map.of("src/Test.java", """
            public class Test {
                public void run() {
                    for (int i = 0; i < 10; i++) {
                        System.out.println(i);
                    }
                }
            }
            """));

        JavaHeuristicAnalyzer analyzer = new JavaHeuristicAnalyzer();
        OptimizationResult optimization = analyzer.optimize(request, java.util.List.of());
        AnalysisResult analysis = analyzer.analyze(request, optimization);

        assertFalse(optimization.getIssues().isEmpty());
        assertTrue(analysis.getComplexity().getLoopCount() > 0);
    }
}
