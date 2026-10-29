package com.swachhdrishti.backend.repository;

import com.swachhdrishti.backend.entity.DisposalLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisposalLogRepository extends JpaRepository<DisposalLog, Long> {

    List<DisposalLog> findTop100ByOrderByDisposedAtDesc();

    List<DisposalLog> findTop100ByIsAuthorisedOrderByDisposedAtDesc(Boolean isAuthorised);

    long countByIsAuthorisedFalse();
}
