package com.cjlogistics.mini.shipment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cargo_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CargoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private Integer weightKg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_request_id", nullable = false)
    private ShipmentRequest shipmentRequest;

    public CargoItem(String description, Integer weightKg) {
        this.description = description;
        this.weightKg = weightKg;
    }

    void assignTo(ShipmentRequest shipmentRequest) {
        this.shipmentRequest = shipmentRequest;
    }
}
