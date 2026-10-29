package com.swachhdrishti.backend.service;

import com.swachhdrishti.backend.entity.Complaint;
import com.swachhdrishti.backend.entity.RewardTransaction;
import com.swachhdrishti.backend.entity.User;
import com.swachhdrishti.backend.repository.RewardTransactionRepository;
import com.swachhdrishti.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RewardService {

    public static final int COINS_PER_VERIFIED_REPORT = 10;

    private final RewardTransactionRepository rewardTransactionRepository;
    private final UserRepository userRepository;

    public RewardService(RewardTransactionRepository rewardTransactionRepository,
                         UserRepository userRepository) {
        this.rewardTransactionRepository = rewardTransactionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Rewards the citizen who reported a complaint once it is genuinely
     * verified as cleaned. Idempotent: a complaint that is reopened and
     * cleaned again never pays out twice.
     *
     * @return true if coins were granted, false if this complaint was already rewarded
     */
    @Transactional
    public boolean rewardForVerifiedReport(Complaint complaint) {
        if (rewardTransactionRepository.existsByComplaint_ComplaintIdAndReason(
                complaint.getComplaintId(), RewardTransaction.Reason.REPORT_VERIFIED)) {
            return false;
        }

        User citizen = complaint.getCitizen();

        RewardTransaction tx = new RewardTransaction();
        tx.setCitizen(citizen);
        tx.setComplaint(complaint);
        tx.setCoinAmount(COINS_PER_VERIFIED_REPORT);
        tx.setReason(RewardTransaction.Reason.REPORT_VERIFIED);
        rewardTransactionRepository.save(tx);

        citizen.setCleanCoins((citizen.getCleanCoins() == null ? 0 : citizen.getCleanCoins()) + COINS_PER_VERIFIED_REPORT);
        userRepository.save(citizen);
        return true;
    }
}
