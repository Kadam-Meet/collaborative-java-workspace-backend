package com.collab.workspace.backend.engine;

import com.collab.workspace.backend.dto.JavaWorkspaceRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class WorkspaceMaterializer {

    public Path materialize(JavaWorkspaceRequest request) throws IOException {
        Path root = Files.createTempDirectory("cjw-" + safeName(request.getWorkspaceName()) + "-");
        for (var entry : request.getFiles().entrySet()) {
            Path file = root.resolve(entry.getKey()).normalize();
            if (!file.startsWith(root)) {
                throw new IOException("Invalid file path: " + entry.getKey());
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        return root;
    }

    public void deleteQuietly(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
        } catch (IOException ignored) {
        }
    }

    private String safeName(String value) {
        return value == null ? "workspace" : value.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}
