package com.cjlogistics.mini.shipment;

import com.cjlogistics.mini.driver.VehicleType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipment_requests", indexes = {@Index(name = "idx_shipment_status", columnList = "status"), @Index(name = "idx_shipment_regions", columnList = "originRegion,destinationRegion")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShipmentRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long shipperId;
    @Column(nullable = false, length = 50) private String originRegion;
    @Column(nullable = false, length = 50) private String destinationRegion;
    @OneToMany(mappedBy = "shipmentRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CargoItem> cargoItems = new ArrayList<>();
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private VehicleType requiredVehicleType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ShipmentStatus status;
    @CreationTimestamp @Column(nullable = false, updatable = false) private LocalDateTime createdAt;

    public ShipmentRequest(Long shipperId, String originRegion, String destinationRegion, List<CargoItem> cargoItems, VehicleType requiredVehicleType) {
        this.shipperId = shipperId; this.originRegion = originRegion; this.destinationRegion = destinationRegion;
        cargoItems.forEach(this::addCargoItem);
        this.requiredVehicleType = requiredVehicleType; this.status = ShipmentStatus.REQUESTED;
    }
    public void addCargoItem(CargoItem cargoItem) { cargoItems.add(cargoItem); cargoItem.assignTo(this); }
    public int getTotalCargoWeightKg() { return cargoItems.stream().mapToInt(CargoItem::getWeightKg).sum(); }
    public void cancel() { if (status != ShipmentStatus.REQUESTED && status != ShipmentStatus.MATCHING) throw new InvalidShipmentStatusTransitionException(status, ShipmentStatus.CANCELED); status = ShipmentStatus.CANCELED; }
    public void startMatching() { if (status != ShipmentStatus.REQUESTED) throw new InvalidShipmentStatusTransitionException(status, ShipmentStatus.MATCHING); status = ShipmentStatus.MATCHING; }
    public void confirmDispatch() { if (status != ShipmentStatus.MATCHING) throw new InvalidShipmentStatusTransitionException(status, ShipmentStatus.DISPATCHED); status = ShipmentStatus.DISPATCHED; }
    public void startPickup() { if (status != ShipmentStatus.DISPATCHED) throw new InvalidShipmentStatusTransitionException(status, ShipmentStatus.EN_ROUTE_TO_PICKUP); status = ShipmentStatus.EN_ROUTE_TO_PICKUP; }
    public void arriveAtPickup() { if (status != ShipmentStatus.EN_ROUTE_TO_PICKUP) throw new InvalidShipmentStatusTransitionException(status, ShipmentStatus.PICKED_UP); status = ShipmentStatus.PICKED_UP; }
    public void startTransit() { if (status != ShipmentStatus.PICKED_UP) throw new InvalidShipmentStatusTransitionException(status, ShipmentStatus.IN_TRANSIT); status = ShipmentStatus.IN_TRANSIT; }
    public void complete() { if (status != ShipmentStatus.IN_TRANSIT) throw new InvalidShipmentStatusTransitionException(status, ShipmentStatus.COMPLETED); status = ShipmentStatus.COMPLETED; }
}
