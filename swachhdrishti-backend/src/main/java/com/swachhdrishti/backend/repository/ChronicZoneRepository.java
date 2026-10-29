package com.swachhdrishti.backend.repository;

import com.swachhdrishti.backend.entity.ChronicZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChronicZoneRepository extends JpaRepository<ChronicZone, Long> {

    List<ChronicZone> findByLatitudeBetweenAndLongitudeBetween(
            Double minLat, Double maxLat, Double minLng, Double maxLng);

    List<ChronicZone> findAllByOrderByRepeatCountDesc();
}
