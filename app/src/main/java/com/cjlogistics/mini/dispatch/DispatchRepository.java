package com.cjlogistics.mini.dispatch;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
    boolean existsByDriverIdAndStatusIn(Long driverId, Collection<DispatchStatus> statuses);
}
