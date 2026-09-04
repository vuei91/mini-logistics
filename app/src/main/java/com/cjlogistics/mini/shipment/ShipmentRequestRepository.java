package com.cjlogistics.mini.shipment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRequestRepository extends JpaRepository<ShipmentRequest, Long> {

    @Override
    @EntityGraph(attributePaths = "cargoItems")
    Optional<ShipmentRequest> findById(Long id);
}
