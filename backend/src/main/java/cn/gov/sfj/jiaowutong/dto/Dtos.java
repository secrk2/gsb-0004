package cn.gov.sfj.jiaowutong.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class Dtos {
    private Dtos() {
    }

    public record LoginReq(String username, String password) {
    }

    public record LoginResp(String token, Long userId, String displayName, String role,
                            String roleLabel, Long officeId, String officeName, Long objectId) {
    }

    public record OfficeDto(Long id, String code, String name, String region) {
    }

    public record ObjectSummaryDto(Long id, String code, String maskedName, String status,
                                   String statusLabel, String crimeType, String officeName,
                                   LocalDate sentenceEnd, boolean openViolations) {
    }

    public record ObjectDetailDto(Long id, String code, String maskedName, String fullName,
                                  String status, String statusLabel, Long officeId, String officeName,
                                  String crimeType, LocalDate sentenceStart, LocalDate sentenceEnd,
                                  String phone, String address, String guardian,
                                  Double homeLat, Double homeLng, Integer fenceRadiusMeters,
                                  boolean nameRevealed, List<String> nextActions,
                                  long openViolationCount) {
    }

    public record StatusChangeReq(String toStatus, String reason) {
    }

    public record RevealNameReq(String reason) {
    }

    public record TimelineEvent(String type, String title, String detail, String operator,
                                Instant at) {
    }

    public record OfficeStat(Long officeId, String officeName,
                             long intake, long active, long leave, long warned,
                             long revoked, long terminated,
                             long total, long dueToday, long overdue, long openViolations) {
    }

    public record DueItem(Long checkInId, Long objectId, String maskedName, String status,
                          String statusLabel, String crimeType) {
    }

    public record RedDotItem(Long violationId, Long objectId, String maskedName,
                             String type, String typeLabel, String detail, Instant occurredAt) {
    }

    public record DashboardResp(List<OfficeStat> offices, List<DueItem> dueToday,
                                List<DueItem> overdue, List<RedDotItem> redDots) {
    }

    public record LocationPointDto(Long id, double lat, double lng, Double accuracy,
                                   Instant recordedAt, Instant reportedAt, String source) {
    }

    public record LocationReportReq(String clientId, Double lat, Double lng, Double accuracy,
                                    Instant recordedAt) {
    }

    public record SyncBatchReq(List<LocationReportReq> points) {
    }

    public record SyncItemResult(String clientId, String outcome, Long pointId, String message) {
    }

    public record SyncBatchResp(int total, int stored, int merged, int rejected,
                                List<SyncItemResult> results) {
    }

    public record LeaveReq(String reason, String destination, LocalDate startDate, LocalDate endDate) {
    }

    public record LeaveDecisionReq(String note) {
    }

    public record LeaveDto(Long id, Long objectId, String reason, String destination,
                           LocalDate startDate, LocalDate endDate, String status,
                           String approver, Instant createdAt) {
    }

    public record CheckInReq() {
    }
}
