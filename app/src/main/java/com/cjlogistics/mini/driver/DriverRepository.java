package com.cjlogistics.mini.driver;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    boolean existsByEmail(String email);
    Optional<Driver> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Driver d where d.id = :id")
    Optional<Driver> findByIdForUpdate(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"vehicle", "preferredRoutes"})
    Optional<Driver> findById(Long id);

    @EntityGraph(attributePaths = {"vehicle", "preferredRoutes"})
    List<Driver> findByStatus(DriverStatus status);
}
