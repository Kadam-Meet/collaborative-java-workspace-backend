package com.collab.workspace.dto;

public class AnalysisRequest {

private String code;
private String language; // optional (java, python, etc.)
private Long roomId;     // optional (if tied to workspace)

public AnalysisRequest() {}

public AnalysisRequest(String code, String language, Long roomId) {
    this.code = code;
    this.language = language;
    this.roomId = roomId;
}

public String getCode() { return code; }
public void setCode(String code) { this.code = code; }

public String getLanguage() { return language; }
public void setLanguage(String language) { this.language = language; }

public Long getRoomId() { return roomId; }
public void setRoomId(Long roomId) { this.roomId = roomId; }

}
