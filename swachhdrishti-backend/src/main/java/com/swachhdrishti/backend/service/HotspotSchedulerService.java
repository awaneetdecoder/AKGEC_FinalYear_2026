package com.swachhdrishti.backend.service;

import com.swachhdrishti.backend.entity.Complaint;
import com.swachhdrishti.backend.entity.HotspotCluster;
import com.swachhdrishti.backend.repository.ComplaintRepository;
import com.swachhdrishti.backend.repository.HotspotClusterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Layer 3 of the accountability system: predicts likely dumping hotspots from
 * the GPS history of complaints using K-Means, once a week by default.
 */
@Service
public class HotspotSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(HotspotSchedulerService.class);
    private static final int MAX_ITERATIONS = 100;

    @Value("${app.hotspot.k:5}")
    private int k;

    @Value("${app.hotspot.min-complaints:5}")
    private int minComplaints;

    private final ComplaintRepository complaintRepository;
    private final HotspotClusterRepository hotspotClusterRepository;
    private final KMeansService kMeansService;

    public HotspotSchedulerService(ComplaintRepository complaintRepository,
                                   HotspotClusterRepository hotspotClusterRepository,
                                   KMeansService kMeansService) {
        this.complaintRepository = complaintRepository;
        this.hotspotClusterRepository = hotspotClusterRepository;
        this.kMeansService = kMeansService;
    }

    /**
     * Weekly batch (also callable on demand by an officer).
     *
     * @return number of hotspot clusters written; 0 if there is not enough data yet
     *         (the existing predictions are then left untouched)
     */
    @Scheduled(fixedRateString = "${app.hotspot.interval-ms:604800000}",
               initialDelayString = "${app.hotspot.initial-delay-ms:120000}")
    @Transactional
    public int generateHotspots() {
        List<KMeansService.Point> points = complaintRepository.findAll().stream()
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .map(c -> new KMeansService.Point(c.getLatitude(), c.getLongitude()))
                .toList();

        if (points.size() < minComplaints) {
            // The paper's known limitation: K-Means needs enough history to be meaningful.
            log.info("Hotspot job skipped: {} complaint(s) < minimum {}.", points.size(), minComplaints);
            return 0;
        }

        List<KMeansService.ClusterResult> clusters = kMeansService.cluster(points, k, MAX_ITERATIONS);

        // Replace the old predictions with this fresh batch.
        hotspotClusterRepository.deleteAllInBatch();

        int written = 0;
        for (KMeansService.ClusterResult cluster : clusters) {
            if (cluster.count() == 0) continue;

            HotspotCluster entity = new HotspotCluster();
            entity.setCenterLatitude(cluster.centerLat());
            entity.setCenterLongitude(cluster.centerLng());
            entity.setComplaintCount(cluster.count());
            entity.setRiskLevel(riskLevelFor(cluster.count()));
            hotspotClusterRepository.save(entity);
            written++;
        }
        log.info("Hotspot job: wrote {} cluster(s) from {} complaint(s).", written, points.size());
        return written;
    }

    // Simple, explainable thresholds on cluster size.
    static HotspotCluster.RiskLevel riskLevelFor(int complaintCount) {
        if (complaintCount >= 20) return HotspotCluster.RiskLevel.CRITICAL;
        if (complaintCount >= 10) return HotspotCluster.RiskLevel.HIGH;
        if (complaintCount >= 4) return HotspotCluster.RiskLevel.MEDIUM;
        return HotspotCluster.RiskLevel.LOW;
    }
}
