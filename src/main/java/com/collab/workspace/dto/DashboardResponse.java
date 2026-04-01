package com.collab.workspace.dto;

import java.util.List;

public class DashboardResponse {


private Totals totals;
private Performance performance;
private List<RoomSummary> rooms;
private List<?> recentActivity;

public DashboardResponse(Totals totals,
                         Performance performance,
                         List<RoomSummary> rooms,
                         List<?> recentActivity) {
    this.totals = totals;
    this.performance = performance;
    this.rooms = rooms;
    this.recentActivity = recentActivity;
}

public Totals getTotals() { return totals; }
public Performance getPerformance() { return performance; }
public List<RoomSummary> getRooms() { return rooms; }
public List<?> getRecentActivity() { return recentActivity; }


}
