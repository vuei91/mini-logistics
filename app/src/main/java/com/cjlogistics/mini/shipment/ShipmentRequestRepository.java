package com.cjlogistics.mini.shipment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentRequestRepository extends JpaRepository<ShipmentRequest, Long> {

    @Override
    @EntityGraph(attributePaths = "cargoItems")
    Optional<ShipmentRequest> findById(Long id);

    @EntityGraph(attributePaths = "cargoItems")
    List<ShipmentRequest> findByShipperIdOrderByCreatedAtDesc(Long shipperId);
}
