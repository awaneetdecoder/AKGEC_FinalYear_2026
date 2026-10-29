package com.swachhdrishti.backend.dto;

import com.swachhdrishti.backend.entity.DisposalLog;

import java.time.LocalDateTime;

public record DisposalLogResponse(Long disposalId, Long complaintId, Long workerId, String workerName,
                                  Double workerLatitude, Double workerLongitude,
                                  boolean authorised, LocalDateTime disposedAt) {

    public static DisposalLogResponse from(DisposalLog d) {
        return new DisposalLogResponse(d.getDisposalId(),
                d.getComplaint().getComplaintId(),
                d.getWorker().getUserId(), d.getWorker().getName(),
                d.getWorkerLatitude(), d.getWorkerLongitude(),
                Boolean.TRUE.equals(d.getIsAuthorised()), d.getDisposedAt());
    }
}
