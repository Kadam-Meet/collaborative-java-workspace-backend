package com.collab.workspace.integration;

import com.collab.workspace.WorkspaceApplication;
import com.collab.workspace.entity.AnalysisReport;
import com.collab.workspace.entity.WorkspaceFile;
import com.collab.workspace.repository.AnalysisReportRepository;
import com.collab.workspace.repository.WorkspaceFileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = WorkspaceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkspaceEndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkspaceFileRepository workspaceFileRepository;

    @Autowired
    private AnalysisReportRepository analysisReportRepository;

    @Test
    void shouldRunAuthRoomFileVersionAndDashboardFlow() throws Exception {
        String ownerEmail = "owner.integration@example.com";
        String ownerPassword = "password123";
        String ownerToken = signupAndExtractToken("Room Owner", ownerEmail, ownerPassword);
        String ownerLoginToken = loginAndExtractToken(ownerEmail, ownerPassword);
        assertThat(ownerLoginToken).isNotBlank();

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", bearer(ownerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(ownerEmail));

        String memberEmail = "member.integration@example.com";
        String memberPassword = "password123";
        String memberToken = signupAndExtractToken("Room Member", memberEmail, memberPassword);
        assertThat(memberToken).isNotBlank();

        JsonNode createdRoom = performJsonOk(post("/api/workspaces/rooms")
            .header("Authorization", bearer(ownerLoginToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("roomName", "Integration Room"))));

        long roomId = createdRoom.path("id").asLong();
        String roomCode = createdRoom.path("roomCode").asText();
        assertThat(roomId).isPositive();
        assertThat(roomCode).isNotBlank();

        performJsonOk(post("/api/workspaces/rooms/join")
            .header("Authorization", bearer(memberToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("roomCode", roomCode))));

        JsonNode createdFile = performJsonOk(post("/api/workspaces/rooms/{roomId}/files", roomId)
            .header("Authorization", bearer(ownerToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "filePath", "Demo.java",
                "language", "java",
                "content", "class Demo { int value = 1; }"
            ))));

        long fileId = createdFile.path("id").asLong();
        JsonNode persistedFile = performJsonOk(get("/api/workspaces/rooms/{roomId}/files/{fileId}", roomId, fileId)
            .header("Authorization", bearer(ownerToken)));
        String expectedUpdatedAt = persistedFile.path("updatedAt").asText();
        assertThat(fileId).isPositive();
        assertThat(expectedUpdatedAt).isNotBlank();

        JsonNode savedFile = performJsonOk(put("/api/workspaces/rooms/{roomId}/files/{fileId}", roomId, fileId)
            .header("Authorization", bearer(ownerToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "filePath", "Demo.java",
                "language", "java",
                "content", "class Demo { int value = 2; }",
                "expectedUpdatedAt", expectedUpdatedAt
            ))));

        String savedUpdatedAt = savedFile.path("updatedAt").asText();
        assertThat(savedUpdatedAt).isNotBlank();

        JsonNode snapshot = performJsonOk(post("/api/workspaces/rooms/{roomId}/files/{fileId}/versions", roomId, fileId)
            .header("Authorization", bearer(ownerToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "content", "class Demo { int value = 3; }",
                "versionMessage", "Integration snapshot"
            ))));

        long versionId = snapshot.path("id").asLong();
        assertThat(versionId).isPositive();

        JsonNode reverted = performJsonOk(post("/api/workspaces/rooms/{roomId}/files/{fileId}/versions/{versionId}/revert", roomId, fileId, versionId)
            .header("Authorization", bearer(ownerToken)));

        assertThat(reverted.path("fileId").asLong()).isEqualTo(fileId);
        assertThat(reverted.path("newVersion").asInt()).isGreaterThan(0);

        WorkspaceFile file = workspaceFileRepository.findById(fileId).orElseThrow();
        AnalysisReport report = new AnalysisReport();
        report.setFile(file);
        report.setCyclomaticComplexity(4);
        report.setTimeComplexity("O(n)");
        report.setPerformanceScore(91);
        report.setRiskLevel("LOW");
        analysisReportRepository.save(report);

        JsonNode dashboard = performJsonOk(get("/api/dashboard")
            .header("Authorization", bearer(ownerToken)));

        assertThat(dashboard.path("totals").path("rooms").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(dashboard.path("totals").path("files").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(dashboard.path("totals").path("versions").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(dashboard.path("totals").path("analyses").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(dashboard.path("performance").path("bestScore").asInt()).isEqualTo(91);
        assertThat(dashboard.path("performance").path("latestRiskLevel").asText()).isEqualTo("LOW");
        assertThat(dashboard.path("recentActivity").isArray()).isTrue();
        assertThat(dashboard.path("recentActivity").size()).isGreaterThan(0);
    }

    @Test
    void shouldRequireTokenForProtectedDashboardEndpoint() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldQueueAnalysisJobAndPollUntilCompleted() throws Exception {
        String ownerToken = signupAndExtractToken("Queue User", "queue.user@example.com", "password123");

        MvcResult queuedResult = mockMvc.perform(post("/api/v1/analyzer/java/jobs")
                .header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "workspaceName", "queue-workspace",
                    "entryFile", "src/DataProcessor.java",
                    "files", Map.of("src/DataProcessor.java", "class DataProcessor { int x = 1; }")
                ))))
            .andExpect(status().isAccepted())
            .andReturn();

        JsonNode queued = objectMapper.readTree(queuedResult.getResponse().getContentAsString());
        String jobId = queued.path("jobId").asText();
        assertThat(jobId).isNotBlank();

        JsonNode latest = null;
        for (int i = 0; i < 40; i++) {
            MvcResult pollResult = mockMvc.perform(get("/api/v1/analyzer/java/jobs/{jobId}", jobId)
                    .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn();

            latest = objectMapper.readTree(pollResult.getResponse().getContentAsString());
            String status = latest.path("status").asText();
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                break;
            }
            Thread.sleep(150);
        }

        assertThat(latest).isNotNull();
        assertThat(latest.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(latest.path("result").path("analysis").path("complexity").path("performanceScore").isInt()).isTrue();
    }

    private String signupAndExtractToken(String name, String email, String password) throws Exception {
        JsonNode node = performJsonOk(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "name", name,
                "email", email,
                "password", password
            ))));
        return node.path("token").asText();
    }

    private String loginAndExtractToken(String email, String password) throws Exception {
        JsonNode node = performJsonOk(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "email", email,
                "password", password
            ))));
        return node.path("token").asText();
    }

    private JsonNode performJsonOk(org.springframework.test.web.servlet.RequestBuilder requestBuilder) throws Exception {
        MvcResult result = mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
