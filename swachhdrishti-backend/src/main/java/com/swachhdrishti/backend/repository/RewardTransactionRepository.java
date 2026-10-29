package com.swachhdrishti.backend.repository;

import com.swachhdrishti.backend.entity.RewardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {

    // Used to make sure a complaint can only ever pay out its reward once.
    boolean existsByComplaint_ComplaintIdAndReason(Long complaintId, RewardTransaction.Reason reason);
}
