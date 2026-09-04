package com.cjlogistics.mini.dispatch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "dispatches",
        indexes = {
                @Index(name = "idx_dispatch_driver", columnList = "driverId"),
                @Index(name = "idx_dispatch_shipment", columnList = "shipmentRequestId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long shipmentRequestId;

    @Column(nullable = false)
    private Long driverId;

    @Column(nullable = false)
    private Double matchScore;

    @Column
    private BigDecimal fare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DispatchStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Dispatch(Long shipmentRequestId, Long driverId, double matchScore) {
        this.shipmentRequestId = shipmentRequestId;
        this.driverId = driverId;
        this.matchScore = matchScore;
        this.status = DispatchStatus.PROPOSED;
    }

    public void accept() {
        if (this.status != DispatchStatus.PROPOSED) {
            throw new InvalidDispatchStatusTransitionException(this.status, DispatchStatus.ACCEPTED);
        }
        this.status = DispatchStatus.ACCEPTED;
    }

    public void reject() {
        if (this.status != DispatchStatus.PROPOSED) {
            throw new InvalidDispatchStatusTransitionException(this.status, DispatchStatus.REJECTED);
        }
        this.status = DispatchStatus.REJECTED;
    }

    public void markCompleted() {
        if (this.status != DispatchStatus.ACCEPTED) {
            throw new InvalidDispatchStatusTransitionException(this.status, DispatchStatus.COMPLETED);
        }
        this.status = DispatchStatus.COMPLETED;
    }
}
