package com.collab.workspace.backend.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteJavaAnalysisService extends Remote {

    String analyzeWorkspace(String workspaceName, String entryFile, String sourceCode) throws RemoteException;
}
