package com.swachhdrishti.backend.repository;

import com.swachhdrishti.backend.entity.DisposalZone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DisposalZoneRepository extends JpaRepository<DisposalZone, Long> {
    List<DisposalZone> findByIsActiveTrue();
}
