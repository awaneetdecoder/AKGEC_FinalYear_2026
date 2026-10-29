package com.swachhdrishti.backend.dto;

import com.swachhdrishti.backend.entity.Complaint;
import com.swachhdrishti.backend.service.SeverityService;

import java.time.LocalDateTime;

/** What we send back after a complaint is created or looked up. */
public record ComplaintResponse(
        Long complaintId,
        String status,
        String garbageType,
        Integer severity,
        Integer slaDeadlineHours,
        LocalDateTime slaDueAt,
        boolean slaBreached,
        Double boundingBoxAreaPct,
        boolean needsReview,
        Double latitude,
        Double longitude,
        String photoUrl,
        Long citizenId,
        Long assignedWorkerId,
        String assignedWorkerName,
        LocalDateTime createdAt,
        LocalDateTime cleanedAt) {

    public static ComplaintResponse from(Complaint c) {
        LocalDateTime due = null;
        if (c.getCreatedAt() != null && c.getSlaDeadlineHours() != null) {
            due = c.getCreatedAt().plusHours(SeverityService.effectiveSlaHours(c.getSlaDeadlineHours()));
        }
        boolean open = c.getStatus() == Complaint.Status.PENDING
                || c.getStatus() == Complaint.Status.ASSIGNED
                || c.getStatus() == Complaint.Status.REOPENED;
        boolean breached = open && due != null && LocalDateTime.now().isAfter(due);

        return new ComplaintResponse(
                c.getComplaintId(),
                c.getStatus().name(),
                c.getGarbageType(),
                c.getSeverity(),
                c.getSlaDeadlineHours(),
                due,
                breached,
                c.getBoundingBoxAreaPct(),
                Boolean.TRUE.equals(c.getNeedsReview()),
                c.getLatitude(),
                c.getLongitude(),
                c.getPhotoUrl(),
                c.getCitizen() == null ? null : c.getCitizen().getUserId(),
                c.getAssignedWorker() == null ? null : c.getAssignedWorker().getUserId(),
                c.getAssignedWorker() == null ? null : c.getAssignedWorker().getName(),
                c.getCreatedAt(),
                c.getCleanedAt());
    }
}
