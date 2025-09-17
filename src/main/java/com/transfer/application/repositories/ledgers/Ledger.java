package com.transfer.application.repositories.ledgers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ledgers")
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "credit", precision = 4)
    private Double credit;

    @Column(name = "debit", precision = 4)
    private Double debit;

    @Column(name = "start_balance", precision = 4, nullable = false)
    private Double startBalance;

    @Column(name = "end_balance", precision = 4)
    private Double endBalance;

    @Column(name = "status", nullable = false)
    private Status status = Status.IN_PROGRESS;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}
