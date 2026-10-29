package com.swachhdrishti.backend.controller;

import com.swachhdrishti.backend.dto.ChronicZoneResponse;
import com.swachhdrishti.backend.dto.DisposalLogResponse;
import com.swachhdrishti.backend.dto.HotspotResponse;
import com.swachhdrishti.backend.entity.Complaint;
import com.swachhdrishti.backend.entity.DisposalLog;
import com.swachhdrishti.backend.repository.ChronicZoneRepository;
import com.swachhdrishti.backend.repository.ComplaintRepository;
import com.swachhdrishti.backend.repository.DisposalLogRepository;
import com.swachhdrishti.backend.repository.HotspotClusterRepository;
import com.swachhdrishti.backend.security.RequestUser;
import com.swachhdrishti.backend.service.HotspotSchedulerService;
import com.swachhdrishti.backend.service.RepeatMonitorSchedulerService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything here is OFFICER-only. */
@RestController
@RequestMapping("/api/officer")
public class OfficerController {

    private final HotspotClusterRepository hotspotClusterRepository;
    private final ComplaintRepository complaintRepository;
    private final ChronicZoneRepository chronicZoneRepository;
    private final DisposalLogRepository disposalLogRepository;
    private final HotspotSchedulerService hotspotSchedulerService;
    private final RepeatMonitorSchedulerService repeatMonitorSchedulerService;

    public OfficerController(HotspotClusterRepository hotspotClusterRepository,
                             ComplaintRepository complaintRepository,
                             ChronicZoneRepository chronicZoneRepository,
                             DisposalLogRepository disposalLogRepository,
                             HotspotSchedulerService hotspotSchedulerService,
                             RepeatMonitorSchedulerService repeatMonitorSchedulerService) {
        this.hotspotClusterRepository = hotspotClusterRepository;
        this.complaintRepository = complaintRepository;
        this.chronicZoneRepository = chronicZoneRepository;
        this.disposalLogRepository = disposalLogRepository;
        this.hotspotSchedulerService = hotspotSchedulerService;
        this.repeatMonitorSchedulerService = repeatMonitorSchedulerService;
    }

    // K-Means hotspot predictions (cluster centre, size, risk level).
    @GetMapping("/hotspots")
    public List<HotspotResponse> hotspots(HttpServletRequest request) {
        RequestUser.from(request).requireRole("OFFICER");
        return hotspotClusterRepository.findAll().stream().map(HotspotResponse::from).toList();
    }

    // Locations that keep re-offending after cleanup (from the 72-hour trap).
    @GetMapping("/chronic-zones")
    public List<ChronicZoneResponse> chronicZones(HttpServletRequest request) {
        RequestUser.from(request).requireRole("OFFICER");
        return chronicZoneRepository.findAllByOrderByRepeatCountDesc().stream()
                .map(ChronicZoneResponse::from).toList();
    }

    // Worker cleanup attempts incl. rejected ones. ?authorised=false shows only rejected attempts.
    @GetMapping("/disposal-logs")
    public List<DisposalLogResponse> disposalLogs(@RequestParam(required = false) Boolean authorised,
                                                  HttpServletRequest request) {
        RequestUser.from(request).requireRole("OFFICER");
        List<DisposalLog> logs = authorised == null
                ? disposalLogRepository.findTop100ByOrderByDisposedAtDesc()
                : disposalLogRepository.findTop100ByIsAuthorisedOrderByDisposedAtDesc(authorised);
        return logs.stream().map(DisposalLogResponse::from).toList();
    }

    // Dashboard summary counters.
    @GetMapping("/stats")
    public Map<String, Long> stats(HttpServletRequest request) {
        RequestUser.from(request).requireRole("OFFICER");
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("pending", complaintRepository.countByStatus(Complaint.Status.PENDING));
        stats.put("assigned", complaintRepository.countByStatus(Complaint.Status.ASSIGNED));
        stats.put("cleaned", complaintRepository.countByStatus(Complaint.Status.CLEANED));
        stats.put("reopened", complaintRepository.countByStatus(Complaint.Status.REOPENED));
        stats.put("closed", complaintRepository.countByStatus(Complaint.Status.CLOSED));
        stats.put("total", complaintRepository.count());
        stats.put("chronicZones", chronicZoneRepository.count());
        stats.put("rejectedCleanupAttempts", disposalLogRepository.countByIsAuthorisedFalse());
        return stats;
    }

    // Run the weekly K-Means job now instead of waiting a week.
    @PostMapping("/hotspots/refresh")
    public Map<String, Integer> refreshHotspots(HttpServletRequest request) {
        RequestUser.from(request).requireRole("OFFICER");
        return Map.of("clustersWritten", hotspotSchedulerService.generateHotspots());
    }

    // Run the 72-hour repeat-complaint check now instead of waiting up to 6 hours.
    @PostMapping("/repeat-monitor/run")
    public Map<String, Integer> runRepeatMonitor(HttpServletRequest request) {
        RequestUser.from(request).requireRole("OFFICER");
        return Map.of("ticketsReopened", repeatMonitorSchedulerService.checkForRepeatComplaints());
    }
}
