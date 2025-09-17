package com.transfer.application.repositories.ledgers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ledgers")
public class Ledger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "credit", precision = 4)
    private Double credit;

    @Column(name = "debit", precision = 4)
    private Double debit;

    @Column(name = "start_balance", precision = 4)
    private Double startBalance;

    @Column(name = "end_balance", precision = 4)
    private Double endBalance;

    @Column(name = "status")
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
