package com.swachhdrishti.backend.service;

import com.swachhdrishti.backend.dto.ComplaintSubmitRequest;
import com.swachhdrishti.backend.entity.Complaint;
import com.swachhdrishti.backend.entity.User;
import com.swachhdrishti.backend.exception.ApiException;
import com.swachhdrishti.backend.repository.ComplaintRepository;
import com.swachhdrishti.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ComplaintService {

    // Reopened tickets first (suspected fake cleanups), then most severe, then oldest.
    private static final Comparator<Complaint> PRIORITY_ORDER = Comparator
            .comparing((Complaint c) -> c.getStatus() == Complaint.Status.REOPENED ? 0 : 1)
            .thenComparing(Comparator.comparing(
                    (Complaint c) -> c.getSeverity() == null ? 0 : c.getSeverity()).reversed())
            .thenComparing(Complaint::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final RoboflowService roboflowService;
    private final SeverityService severityService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            UserRepository userRepository,
                            RoboflowService roboflowService,
                            SeverityService severityService) {
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.roboflowService = roboflowService;
        this.severityService = severityService;
    }

    /**
     * The submit pipeline: photo -> AI classification -> severity + SLA ->
     * saved to the database (steps 1-5 of the paper's methodology).
     */
    @Transactional
    public Complaint submit(Long citizenId, ComplaintSubmitRequest request) {
        User citizen = userRepository.findById(citizenId)
                .orElseThrow(() -> new ApiException(404, "Citizen not found"));

        RoboflowService.DetectionResult detection = roboflowService.classify(request.getPhotoUrl());

        // Not confident -> documented fallback: severity 3, flagged for human review.
        int severity = detection.confident()
                ? severityService.calculateSeverity(detection.boundingBoxAreaPct())
                : SeverityService.FALLBACK_SEVERITY;

        Complaint complaint = new Complaint();
        complaint.setCitizen(citizen);
        complaint.setPhotoUrl(request.getPhotoUrl());
        complaint.setLatitude(request.getLatitude());
        complaint.setLongitude(request.getLongitude());
        complaint.setGarbageType(detection.garbageType());
        complaint.setBoundingBoxAreaPct(detection.boundingBoxAreaPct());
        complaint.setSeverity(severity);
        complaint.setSlaDeadlineHours(severityService.calculateSlaHours(severity));
        complaint.setNeedsReview(!detection.confident());
        complaint.setStatus(Complaint.Status.PENDING);

        Complaint saved = complaintRepository.save(complaint);

        citizen.setTotalReports((citizen.getTotalReports() == null ? 0 : citizen.getTotalReports()) + 1);
        userRepository.save(citizen);

        return saved;
    }

    /** Priority queue for staff: optional status filter, reopened + severe first. */
    @Transactional(readOnly = true)
    public List<Complaint> getQueue(Complaint.Status status) {
        List<Complaint> list = status != null
                ? complaintRepository.findByStatus(status)
                : complaintRepository.findAll();
        return list.stream().sorted(PRIORITY_ORDER).toList();
    }

    @Transactional(readOnly = true)
    public List<Complaint> getMine(Long citizenId) {
        return complaintRepository.findByCitizen_UserIdOrderByCreatedAtDesc(citizenId);
    }

    @Transactional(readOnly = true)
    public Complaint getById(Long complaintId) {
        return complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ApiException(404, "Complaint not found"));
    }
}
