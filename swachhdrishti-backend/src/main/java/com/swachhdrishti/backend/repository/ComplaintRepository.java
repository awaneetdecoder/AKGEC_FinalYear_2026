package com.swachhdrishti.backend.repository;

import com.swachhdrishti.backend.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStatus(Complaint.Status status);

    long countByStatus(Complaint.Status status);

    List<Complaint> findByCitizen_UserIdOrderByCreatedAtDesc(Long citizenId);

    // Rough pre-filter using a lat/lng bounding box (fast, uses no trig).
    // The exact distance is then verified precisely in Java using the
    // Haversine formula in HaversineService.
    List<Complaint> findByLatitudeBetweenAndLongitudeBetweenAndCreatedAtAfter(
            Double minLat, Double maxLat, Double minLng, Double maxLng, LocalDateTime after);
}
