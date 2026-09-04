package com.cjlogistics.mini.driver;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    @Override
    @EntityGraph(attributePaths = {"vehicle", "preferredRoutes"})
    Optional<Driver> findById(Long id);

    @EntityGraph(attributePaths = {"vehicle", "preferredRoutes"})
    List<Driver> findByStatus(DriverStatus status);
}
