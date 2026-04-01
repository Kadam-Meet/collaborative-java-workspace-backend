package com.collab.workspace.service;

import com.collab.workspace.dto.DashboardResponse;
import com.collab.workspace.dto.Performance;
import com.collab.workspace.dto.RoomSummary;
import com.collab.workspace.dto.Totals;
import com.collab.workspace.entity.AnalysisReport;
import com.collab.workspace.entity.Room;
import com.collab.workspace.entity.User;
import com.collab.workspace.repository.AnalysisReportRepository;
import com.collab.workspace.repository.RoomMemberRepository;
import com.collab.workspace.repository.RoomRepository;
import com.collab.workspace.repository.UserRepository;
import com.collab.workspace.repository.VersionRepository;
import com.collab.workspace.repository.WorkspaceFileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

@Service
public class DashboardService {


private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

private final UserRepository userRepository;
private final RoomRepository roomRepository;
private final RoomMemberRepository roomMemberRepository;
private final WorkspaceFileRepository workspaceFileRepository;
private final VersionRepository versionRepository;
private final AnalysisReportRepository analysisReportRepository;
private final ActivityEventService activityEventService;

public DashboardService(
	UserRepository userRepository,
	RoomRepository roomRepository,
	RoomMemberRepository roomMemberRepository,
	WorkspaceFileRepository workspaceFileRepository,
	VersionRepository versionRepository,
	AnalysisReportRepository analysisReportRepository,
	ActivityEventService activityEventService
) {
	this.userRepository = userRepository;
	this.roomRepository = roomRepository;
	this.roomMemberRepository = roomMemberRepository;
	this.workspaceFileRepository = workspaceFileRepository;
	this.versionRepository = versionRepository;
	this.analysisReportRepository = analysisReportRepository;
	this.activityEventService = activityEventService;
}

@Transactional(readOnly = true)
public DashboardResponse getDashboard(String currentUserEmail) {

	log.info("Fetching dashboard for user: {}", currentUserEmail);

	User currentUser = getUserByEmail(currentUserEmail);

	List<Room> rooms = roomRepository.findAllByParticipantUserId(currentUser.getId());
	List<Long> roomIds = rooms.stream().map(Room::getId).toList();

	log.debug("User {} is part of {} rooms", currentUserEmail, rooms.size());

	long totalRooms = rooms.size();
	long totalFiles = roomIds.isEmpty() ? 0L : workspaceFileRepository.countByRoom_IdIn(roomIds);
	long totalVersions = roomIds.isEmpty() ? 0L : versionRepository.countByFile_Room_IdIn(roomIds);
	long roomScopedAnalyses = roomIds.isEmpty() ? 0L : analysisReportRepository.countByFile_Room_IdIn(roomIds);
	long totalAnalyses = roomScopedAnalyses > 0 ? roomScopedAnalyses : analysisReportRepository.count();

	log.debug("Stats -> rooms: {}, files: {}, versions: {}, analyses: {}",
			totalRooms, totalFiles, totalVersions, totalAnalyses);

	Double avgScoreDb = roomIds.isEmpty() ? null : analysisReportRepository.averagePerformanceByRoomIds(roomIds);
	Integer bestScoreDb = roomIds.isEmpty() ? null : analysisReportRepository.maxPerformanceByRoomIds(roomIds);
	AnalysisReport latestRoomReport = roomIds.isEmpty() ? null : analysisReportRepository.findTopByFile_Room_IdInOrderByCreatedAtDesc(roomIds);
	AnalysisReport latestGlobalReport = analysisReportRepository.findTopByOrderByCreatedAtDesc().orElse(null);

	double averagePerformance = avgScoreDb == null
		? (latestGlobalReport == null ? 0.0 : latestGlobalReport.getPerformanceScore())
		: avgScoreDb;

	int bestPerformance = bestScoreDb == null
		? (latestGlobalReport == null ? 0 : latestGlobalReport.getPerformanceScore())
		: bestScoreDb;

	AnalysisReport riskSource = latestRoomReport != null ? latestRoomReport : latestGlobalReport;

	String latestRiskLevel = riskSource == null || riskSource.getRiskLevel() == null
		? "UNKNOWN"
		: riskSource.getRiskLevel();

	log.debug("Performance -> avg: {}, best: {}, risk: {}",
			averagePerformance, bestPerformance, latestRiskLevel);

	List<RoomSummary> roomSummaries = rooms.stream().map(this::toRoomSummary).toList();
	List<?> recentActivity = activityEventService.listUserActivity(roomIds);

	Totals totals = new Totals(totalRooms, totalFiles, totalVersions, totalAnalyses);

	Performance performance = new Performance(
		Math.round(averagePerformance * 100.0) / 100.0,
		bestPerformance,
		latestRiskLevel
	);

	log.info("Dashboard successfully generated for user: {}", currentUserEmail);

	return new DashboardResponse(totals, performance, roomSummaries, recentActivity);
}

private RoomSummary toRoomSummary(Room room) {
	return new RoomSummary(
		room.getId(),
		room.getRoomCode(),
		room.getRoomName(),
		room.getCreatedAt(),
		room.getOwner() != null ? room.getOwner().getEmail() : null,
		roomMemberRepository.countByRoom_Id(room.getId()),
		workspaceFileRepository.countByRoom_Id(room.getId())
	);
}

private User getUserByEmail(String email) {

	if (email == null || email.isBlank()) {
		log.warn("Attempt to fetch dashboard with missing email");
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Authenticated user email is missing");
	}

	String normalized = email.trim().toLowerCase(Locale.ROOT);

	return userRepository.findByEmailIgnoreCase(normalized)
		.orElseThrow(() -> {
			log.warn("User not found for email: {}", email);
			return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
		});
}

}
