package com.cjlogistics.mini.shipper;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShipperRepository extends JpaRepository<Shipper, Long> {
    boolean existsByEmail(String email);
    Optional<Shipper> findByEmail(String email);
}
