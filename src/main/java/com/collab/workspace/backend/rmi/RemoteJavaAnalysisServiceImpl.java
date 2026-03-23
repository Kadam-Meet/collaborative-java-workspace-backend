package com.collab.workspace.backend.rmi;

import com.collab.workspace.backend.dto.JavaWorkspaceRequest;
import com.collab.workspace.backend.model.FullReviewResponse;
import com.collab.workspace.backend.service.JavaWorkspaceReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class RemoteJavaAnalysisServiceImpl extends UnicastRemoteObject implements RemoteJavaAnalysisService {

    private final JavaWorkspaceReviewService reviewService;
    private final ObjectMapper objectMapper;

    public RemoteJavaAnalysisServiceImpl(JavaWorkspaceReviewService reviewService, ObjectMapper objectMapper) throws RemoteException {
        this.reviewService = reviewService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String analyzeWorkspace(String workspaceName, String entryFile, String sourceCode) throws RemoteException {
        JavaWorkspaceRequest request = new JavaWorkspaceRequest();
        request.setWorkspaceName(workspaceName);
        request.setEntryFile(entryFile);
        request.setFiles(Map.of(entryFile, sourceCode));

        FullReviewResponse response = reviewService.fullReview(request);
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new RemoteException("Unable to serialize analysis response", ex);
        }
    }
}
