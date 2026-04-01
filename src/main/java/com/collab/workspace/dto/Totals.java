package com.collab.workspace.dto;

public class Totals {

private long rooms;
private long files;
private long versions;
private long analyses;

public Totals(long rooms, long files, long versions, long analyses) {
    this.rooms = rooms;
    this.files = files;
    this.versions = versions;
    this.analyses = analyses;
}

public long getRooms() { return rooms; }
public long getFiles() { return files; }
public long getVersions() { return versions; }
public long getAnalyses() { return analyses; }

}
