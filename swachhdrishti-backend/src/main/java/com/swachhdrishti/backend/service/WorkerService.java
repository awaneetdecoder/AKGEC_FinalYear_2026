package com.swachhdrishti.backend.service;

import com.swachhdrishti.backend.dto.WorkerVerifyRequest;
import com.swachhdrishti.backend.dto.WorkerVerifyResponse;
import com.swachhdrishti.backend.entity.Complaint;
import com.swachhdrishti.backend.entity.DisposalLog;
import com.swachhdrishti.backend.entity.DisposalZone;
import com.swachhdrishti.backend.entity.RepeatMonitor;
import com.swachhdrishti.backend.entity.User;
import com.swachhdrishti.backend.exception.ApiException;
import com.swachhdrishti.backend.repository.ComplaintRepository;
import com.swachhdrishti.backend.repository.DisposalLogRepository;
import com.swachhdrishti.backend.repository.DisposalZoneRepository;
import com.swachhdrishti.backend.repository.RepeatMonitorRepository;
import com.swachhdrishti.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Layer 1 of the accountability system: the GPS geofence on "mark as cleaned". */
@Service
public class WorkerService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final DisposalZoneRepository disposalZoneRepository;
    private final DisposalLogRepository disposalLogRepository;
    private final RepeatMonitorRepository repeatMonitorRepository;
    private final HaversineService haversineService;
    private final RewardService rewardService;

    @Value("${app.repeat-monitor.window-hours:72}")
    private long monitorWindowHours;

    public WorkerService(ComplaintRepository complaintRepository,
                         UserRepository userRepository,
                         DisposalZoneRepository disposalZoneRepository,
                         DisposalLogRepository disposalLogRepository,
                         RepeatMonitorRepository repeatMonitorRepository,
                         HaversineService haversineService,
                         RewardService rewardService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.disposalZoneRepository = disposalZoneRepository;
        this.disposalLogRepository = disposalLogRepository;
        this.repeatMonitorRepository = repeatMonitorRepository;
        this.haversineService = haversineService;
        this.rewardService = rewardService;
    }

    /** A worker claims an open complaint (PENDING / REOPENED -> ASSIGNED). Idempotent for the same worker. */
    @Transactional
    public Complaint assign(Long workerId, Long complaintId) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new ApiException(404, "Worker not found"));
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ApiException(404, "Complaint not found"));

        switch (complaint.getStatus()) {
            case CLEANED, CLOSED -> throw new ApiException(409, "This complaint is already " + complaint.getStatus() + ".");
            case ASSIGNED -> {
                if (complaint.getAssignedWorker() != null && !complaint.getAssignedWorker().getUserId().equals(workerId)) {
                    throw new ApiException(409, "This complaint is already assigned to "
                            + complaint.getAssignedWorker().getName() + ".");
                }
            }
            default -> { /* PENDING or REOPENED: free to claim */ }
        }

        complaint.setAssignedWorker(worker);
        complaint.setStatus(Complaint.Status.ASSIGNED);
        return complaintRepository.save(complaint);
    }

    /**
     * Verifies a cleanup. The Haversine distance is computed HERE, on the
     * server, so a client cannot alter the result. The attempt is always
     * logged - authorised or not - which keeps the evidence trail honest.
     */
    @Transactional
    public WorkerVerifyResponse verifyCleanup(Long workerId, WorkerVerifyRequest request) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new ApiException(404, "Worker not found"));

        Complaint complaint = complaintRepository.findById(request.getComplaintId())
                .orElseThrow(() -> new ApiException(404, "Complaint not found"));

        if (complaint.getStatus() == Complaint.Status.CLEANED || complaint.getStatus() == Complaint.Status.CLOSED) {
            throw new ApiException(409, "This complaint is already " + complaint.getStatus() + ".");
        }
        if (complaint.getStatus() == Complaint.Status.ASSIGNED
                && complaint.getAssignedWorker() != null
                && !complaint.getAssignedWorker().getUserId().equals(workerId)) {
            throw new ApiException(409, "This complaint is assigned to "
                    + complaint.getAssignedWorker().getName() + ".");
        }

        List<DisposalZone> zones = disposalZoneRepository.findByIsActiveTrue();
        if (zones.isEmpty()) {
            throw new ApiException(500, "No authorised disposal zones configured. Ask an admin to seed disposal_zones.");
        }

        // Nearest authorised zone to the worker's current GPS.
        DisposalZone nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (DisposalZone zone : zones) {
            double d = haversineService.distanceInMeters(
                    request.getWorkerLatitude(), request.getWorkerLongitude(),
                    zone.getLatitude(), zone.getLongitude());
            if (d < nearestDistance) {
                nearestDistance = d;
                nearest = zone;
            }
        }

        double distance = Math.round(nearestDistance * 10) / 10.0;
        boolean isAuthorised = nearestDistance <= nearest.getRadiusMeters();

        DisposalLog log = new DisposalLog();
        log.setComplaint(complaint);
        log.setWorker(worker);
        log.setWorkerLatitude(request.getWorkerLatitude());
        log.setWorkerLongitude(request.getWorkerLongitude());
        log.setIsAuthorised(isAuthorised);
        disposalLogRepository.save(log);

        if (!isAuthorised) {
            return new WorkerVerifyResponse(false, distance, nearest.getZoneName(),
                    "Too far from " + nearest.getZoneName() + " (" + distance + " m). You must be within "
                            + nearest.getRadiusMeters() + " m to confirm this cleanup.");
        }

        // GPS check passed: mark cleaned, record the worker, reward the citizen,
        // and start the 72-hour fraud watch (Layer 2).
        LocalDateTime now = LocalDateTime.now();
        complaint.setStatus(Complaint.Status.CLEANED);
        complaint.setAssignedWorker(worker);
        complaint.setCleanedAt(now);
        complaintRepository.save(complaint);

        rewardService.rewardForVerifiedReport(complaint);

        RepeatMonitor monitor = new RepeatMonitor();
        monitor.setComplaint(complaint);
        monitor.setLatitude(complaint.getLatitude());
        monitor.setLongitude(complaint.getLongitude());
        monitor.setClosedAt(now);
        monitor.setExpiryAt(now.plusHours(monitorWindowHours));
        repeatMonitorRepository.save(monitor);

        return new WorkerVerifyResponse(true, distance, nearest.getZoneName(),
                "Cleanup verified at " + nearest.getZoneName() + ". Complaint marked as cleaned.");
    }
}
