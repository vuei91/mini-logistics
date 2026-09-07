package com.cjlogistics.mini.dispatch;

import com.cjlogistics.mini.dispatch.event.DispatchConfirmedEvent;
import com.cjlogistics.mini.dispatch.event.OutboxEventStore;
import com.cjlogistics.mini.dispatch.event.ShipmentCompletedEvent;
import com.cjlogistics.mini.driver.Driver;
import com.cjlogistics.mini.driver.DriverRepository;
import com.cjlogistics.mini.driver.DriverService;
import com.cjlogistics.mini.driver.DriverStatus;
import com.cjlogistics.mini.shipment.ShipmentRequest;
import com.cjlogistics.mini.shipment.ShipmentRequestService;
import com.cjlogistics.mini.shipment.ShipmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DispatchService {

    private final ShipmentRequestService shipmentRequestService;
    private final DriverRepository driverRepository;
    private final DriverService driverService;
    private final DispatchRepository dispatchRepository;
    private final MatchingStrategy matchingStrategy;
    private final OutboxEventStore outboxEventStore;
    private final FareCalculator fareCalculator;

    /**
     * 매칭 후보 기사 목록을 조회한다. (상태 변경 없음, 읽기 전용)
     */
    public List<MatchCandidate> findCandidates(Long shipmentRequestId) {
        ShipmentRequest request = shipmentRequestService.get(shipmentRequestId);
        List<Driver> availableDrivers = driverRepository.findByStatus(DriverStatus.AVAILABLE);
        return matchingStrategy.findCandidates(request, availableDrivers);
    }

    /**
     * 최적 후보(1등)를 자동 선택해 배차한다. (하위 호환용)
     */
    @Transactional
    public Dispatch matchAndDispatch(Long shipmentRequestId) {
        return matchAndDispatch(shipmentRequestId, null);
    }

    /**
     * 배차를 생성한다.
     * @param targetDriverId 화주가 지정한 기사 ID. null 이면 최적 후보(1등)를 자동 선택.
     */
    @Transactional
    public Dispatch matchAndDispatch(Long shipmentRequestId, Long targetDriverId) {
        ShipmentRequest request = shipmentRequestService.get(shipmentRequestId);

        List<Driver> availableDrivers = driverRepository.findByStatus(DriverStatus.AVAILABLE);
        List<MatchCandidate> candidates = matchingStrategy.findCandidates(request, availableDrivers);

        if (candidates.isEmpty()) {
            throw new NoMatchingDriverException(shipmentRequestId);
        }

        // 지정 기사가 있으면 후보 목록에서 선택, 없으면 1등 자동 선택
        MatchCandidate chosen;
        if (targetDriverId != null) {
            chosen = candidates.stream()
                    .filter(c -> c.driver().getId().equals(targetDriverId))
                    .findFirst()
                    .orElseThrow(() -> new NoMatchingDriverException(shipmentRequestId));
        } else {
            chosen = candidates.get(0);
        }

        request.startMatching();

        Driver lockedDriver = driverRepository.findByIdForUpdate(chosen.driver().getId())
                .orElseThrow(() -> new NoMatchingDriverException(shipmentRequestId));
        if (lockedDriver.getStatus() != DriverStatus.AVAILABLE || dispatchRepository.existsByDriverIdAndStatusIn(lockedDriver.getId(), List.of(DispatchStatus.PROPOSED, DispatchStatus.ACCEPTED))) {
            throw new DriverAlreadyAssignedException(lockedDriver.getId());
        }
        BigDecimal fare = fareCalculator.calculate(request);
        Dispatch dispatch = new Dispatch(request.getId(), chosen.driver().getId(), chosen.score(), fare);
        return dispatchRepository.save(dispatch);
    }

    /** 화물 요청의 예상 운임 (후보 조회 시 노출용) */
    public BigDecimal estimateFare(Long shipmentRequestId) {
        return fareCalculator.calculate(shipmentRequestService.get(shipmentRequestId));
    }

    public Dispatch get(Long id) {
        return dispatchRepository.findById(id)
                .orElseThrow(() -> new DispatchNotFoundException(id));
    }

    public List<Dispatch> getByDriver(Long driverId) {
        return dispatchRepository.findByDriverIdOrderByCreatedAtDesc(driverId);
    }

    public void verifyDriverOwnership(Long dispatchId, Long driverId) {
        if (!get(dispatchId).getDriverId().equals(driverId)) throw new DispatchAccessDeniedException(dispatchId);
    }

    @Transactional
    public Dispatch accept(Long dispatchId) {
        Dispatch dispatch = get(dispatchId);
        dispatch.accept();

        ShipmentRequest request = shipmentRequestService.get(dispatch.getShipmentRequestId());
        request.confirmDispatch();

        Driver driver = driverService.get(dispatch.getDriverId());
        driver.updateStatus(DriverStatus.BUSY);
        outboxEventStore.store(new DispatchConfirmedEvent(
                dispatch.getId(), request.getId(), driver.getId(), LocalDateTime.now()));

        return dispatch;
    }

    @Transactional
    public Dispatch reject(Long dispatchId) {
        Dispatch dispatch = get(dispatchId);
        dispatch.reject();
        return dispatch;
    }

    @Transactional
    public Dispatch updateShipmentStatus(Long dispatchId, ShipmentStatus target) {
        Dispatch dispatch = get(dispatchId);
        if (dispatch.getStatus() != DispatchStatus.ACCEPTED) {
            throw new InvalidDispatchStatusTransitionException(dispatch.getStatus(), DispatchStatus.ACCEPTED);
        }
        ShipmentRequest request = shipmentRequestService.get(dispatch.getShipmentRequestId());

        switch (target) {
            case EN_ROUTE_TO_PICKUP -> request.startPickup();
            case PICKED_UP -> request.arriveAtPickup();
            case IN_TRANSIT -> request.startTransit();
            case COMPLETED -> {
                request.complete();
                dispatch.markCompleted();
                Driver driver = driverService.get(dispatch.getDriverId());
                driver.updateStatus(DriverStatus.AVAILABLE);
                outboxEventStore.store(new ShipmentCompletedEvent(
                        dispatch.getId(), request.getId(), driver.getId(), LocalDateTime.now()));
            }
            default -> throw new IllegalStatusTargetException(target);
        }
        return dispatch;
    }
}
