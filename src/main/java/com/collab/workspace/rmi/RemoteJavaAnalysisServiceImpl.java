package com.collab.workspace.rmi;

import com.collab.workspace.analysis.model.FullReviewResponse;
import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.service.WorkspaceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class RemoteJavaAnalysisServiceImpl extends UnicastRemoteObject implements RemoteJavaAnalysisService {

    private final WorkspaceService reviewService;
    private final ObjectMapper objectMapper;

    public RemoteJavaAnalysisServiceImpl(WorkspaceService reviewService, ObjectMapper objectMapper) throws RemoteException {
        this.reviewService = reviewService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String analyzeWorkspace(String workspaceName, String entryFile, String sourceCode) throws RemoteException {
        WorkspaceRequest request = new WorkspaceRequest();
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
