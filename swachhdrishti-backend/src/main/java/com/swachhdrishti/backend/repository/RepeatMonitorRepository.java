package com.swachhdrishti.backend.repository;

import com.swachhdrishti.backend.entity.RepeatMonitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RepeatMonitorRepository extends JpaRepository<RepeatMonitor, Long> {

    // Monitors that have not fired yet and whose window has not ended
    // (callers pass "now - grace" so a window gets one last check after expiry).
    List<RepeatMonitor> findByTriggeredFalseAndExpiryAtAfter(LocalDateTime cutoff);

    // Previously triggered monitors near a point (bounding-box pre-filter).
    List<RepeatMonitor> findByTriggeredTrueAndLatitudeBetweenAndLongitudeBetween(
            Double minLat, Double maxLat, Double minLng, Double maxLng);
}
