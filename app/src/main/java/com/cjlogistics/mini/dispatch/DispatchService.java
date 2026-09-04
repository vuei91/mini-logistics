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

    @Transactional
    public Dispatch matchAndDispatch(Long shipmentRequestId) {
        ShipmentRequest request = shipmentRequestService.get(shipmentRequestId);

        List<Driver> availableDrivers = driverRepository.findByStatus(DriverStatus.AVAILABLE);
        List<MatchCandidate> candidates = matchingStrategy.findCandidates(request, availableDrivers);

        if (candidates.isEmpty()) {
            throw new NoMatchingDriverException(shipmentRequestId);
        }

        request.startMatching();

        MatchCandidate best = candidates.get(0);
        Driver lockedDriver = driverRepository.findByIdForUpdate(best.driver().getId()).orElseThrow(() -> new NoMatchingDriverException(shipmentRequestId));
        if (lockedDriver.getStatus() != DriverStatus.AVAILABLE || dispatchRepository.existsByDriverIdAndStatusIn(lockedDriver.getId(), List.of(DispatchStatus.PROPOSED, DispatchStatus.ACCEPTED))) {
            throw new DriverAlreadyAssignedException(lockedDriver.getId());
        }
        Dispatch dispatch = new Dispatch(request.getId(), best.driver().getId(), best.score());
        return dispatchRepository.save(dispatch);
    }

    public Dispatch get(Long id) {
        return dispatchRepository.findById(id)
                .orElseThrow(() -> new DispatchNotFoundException(id));
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
