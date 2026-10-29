package com.swachhdrishti.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// Matches "reward_transactions". Every Clean Coin ever given to a citizen.
@Entity
@Table(name = "reward_transactions")
@Getter
@Setter
@NoArgsConstructor
public class RewardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    @ManyToOne
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;

    private Integer coinAmount;

    @Enumerated(EnumType.STRING)
    private Reason reason;

    private LocalDateTime givenAt = LocalDateTime.now();

    public enum Reason {
        REPORT_VERIFIED, WEEKLY_BONUS, CHALLENGE_WIN
    }
}
