package com.swachhdrishti.backend.service;

import com.swachhdrishti.backend.entity.ChronicZone;
import com.swachhdrishti.backend.entity.Complaint;
import com.swachhdrishti.backend.entity.RepeatMonitor;
import com.swachhdrishti.backend.repository.ChronicZoneRepository;
import com.swachhdrishti.backend.repository.ComplaintRepository;
import com.swachhdrishti.backend.repository.RepeatMonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Layer 2 of the accountability system - the 72-hour repeat-complaint trap.
 *
 * Every closed complaint gets a RepeatMonitor row (location + closing time +
 * 72 h expiry). Every few hours this job looks for a NEW complaint filed
 * within 50 m of that spot, after the closure, inside the window. If one
 * exists the original ticket is reopened (at the top of the officer queue),
 * and locations that keep re-triggering are written to chronic_zones.
 *
 * The job never punishes a worker automatically: it only surfaces evidence
 * for a human officer to review.
 */
@Service
public class RepeatMonitorSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(RepeatMonitorSchedulerService.class);

    private static final double METERS_PER_DEGREE_LAT = 111_000.0;

    @Value("${app.repeat-monitor.radius-meters:50}")
    private double radiusMeters;

    @Value("${app.repeat-monitor.grace-hours:6}")
    private long graceHours;

    @Value("${app.repeat-monitor.chronic-threshold:2}")
    private int chronicThreshold;

    private final RepeatMonitorRepository repeatMonitorRepository;
    private final ComplaintRepository complaintRepository;
    private final ChronicZoneRepository chronicZoneRepository;
    private final HaversineService haversineService;

    public RepeatMonitorSchedulerService(RepeatMonitorRepository repeatMonitorRepository,
                                         ComplaintRepository complaintRepository,
                                         ChronicZoneRepository chronicZoneRepository,
                                         HaversineService haversineService) {
        this.repeatMonitorRepository = repeatMonitorRepository;
        this.complaintRepository = complaintRepository;
        this.chronicZoneRepository = chronicZoneRepository;
        this.haversineService = haversineService;
    }

    /**
     * Runs every 6 hours by default. Also callable on demand (officer
     * endpoint) - Spring's proxy makes this transactional either way.
     *
     * @return how many closed tickets were reopened in this run
     */
    @Scheduled(fixedRateString = "${app.repeat-monitor.interval-ms:21600000}",
               initialDelayString = "${app.repeat-monitor.initial-delay-ms:60000}")
    @Transactional
    public int checkForRepeatComplaints() {
        // "now - grace": a monitor whose window just ended still gets one final check.
        List<RepeatMonitor> candidates = repeatMonitorRepository
                .findByTriggeredFalseAndExpiryAtAfter(LocalDateTime.now().minusHours(graceHours));

        int reopened = 0;
        for (RepeatMonitor monitor : candidates) {
            if (processMonitor(monitor)) reopened++;
        }
        if (reopened > 0) {
            log.info("Repeat-complaint trap: reopened {} ticket(s).", reopened);
        }
        return reopened;
    }

    private boolean processMonitor(RepeatMonitor monitor) {
        Complaint original = monitor.getComplaint();
        LocalDateTime since = monitor.getClosedAt() != null ? monitor.getClosedAt() : original.getCreatedAt();

        double latBox = radiusMeters / METERS_PER_DEGREE_LAT;
        // A degree of longitude gets shorter away from the equator.
        double lngBox = latBox / Math.max(0.01, Math.cos(Math.toRadians(monitor.getLatitude())));

        List<Complaint> nearby = complaintRepository
                .findByLatitudeBetweenAndLongitudeBetweenAndCreatedAtAfter(
                        monitor.getLatitude() - latBox, monitor.getLatitude() + latBox,
                        monitor.getLongitude() - lngBox, monitor.getLongitude() + lngBox,
                        since);

        for (Complaint candidate : nearby) {
            if (candidate.getComplaintId().equals(original.getComplaintId())) continue;
            if (candidate.getCreatedAt().isAfter(monitor.getExpiryAt())) continue; // outside the window

            double distance = haversineService.distanceInMeters(
                    monitor.getLatitude(), monitor.getLongitude(),
                    candidate.getLatitude(), candidate.getLongitude());

            if (distance <= radiusMeters) {
                flagAsSuspicious(monitor);
                return true;
            }
        }
        return false;
    }

    private void flagAsSuspicious(RepeatMonitor monitor) {
        Complaint original = monitor.getComplaint();
        original.setStatus(Complaint.Status.REOPENED);
        complaintRepository.save(original);

        monitor.setTriggered(true);
        repeatMonitorRepository.save(monitor);

        updateChronicZone(monitor);
    }

    // A spot is "chronic" once it has re-triggered the trap `chronicThreshold` times.
    private void updateChronicZone(RepeatMonitor monitor) {
        double latBox = radiusMeters / METERS_PER_DEGREE_LAT;
        double lngBox = latBox / Math.max(0.01, Math.cos(Math.toRadians(monitor.getLatitude())));
        double minLat = monitor.getLatitude() - latBox, maxLat = monitor.getLatitude() + latBox;
        double minLng = monitor.getLongitude() - lngBox, maxLng = monitor.getLongitude() + lngBox;

        long triggersHere = repeatMonitorRepository
                .findByTriggeredTrueAndLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLng, maxLng)
                .stream()
                .filter(m -> haversineService.isWithinRadius(
                        monitor.getLatitude(), monitor.getLongitude(),
                        m.getLatitude(), m.getLongitude(), radiusMeters))
                .count();

        if (triggersHere < chronicThreshold) return;

        ChronicZone zone = chronicZoneRepository
                .findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLng, maxLng)
                .stream().findFirst().orElseGet(() -> {
                    ChronicZone z = new ChronicZone();
                    z.setLatitude(monitor.getLatitude());
                    z.setLongitude(monitor.getLongitude());
                    return z;
                });
        zone.setRepeatCount((int) triggersHere);
        zone.setFlaggedAt(LocalDateTime.now());
        chronicZoneRepository.save(zone);
    }
}
