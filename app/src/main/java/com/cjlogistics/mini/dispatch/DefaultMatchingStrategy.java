package com.cjlogistics.mini.dispatch;

import com.cjlogistics.mini.driver.Driver;
import com.cjlogistics.mini.driver.DriverStatus;
import com.cjlogistics.mini.driver.PreferredRoute;
import com.cjlogistics.mini.driver.Vehicle;
import com.cjlogistics.mini.shipment.ShipmentRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class DefaultMatchingStrategy implements MatchingStrategy {

    private static final double BASE_SCORE = 100.0;
    private static final double PREFERRED_ROUTE_BONUS = 30.0;

    @Override
    public List<MatchCandidate> findCandidates(ShipmentRequest request, List<Driver> availableDrivers) {
        return availableDrivers.stream()
                .filter(d -> d.getStatus() == DriverStatus.AVAILABLE)
                .filter(d -> matchesVehicle(d.getVehicle(), request))
                .map(d -> new MatchCandidate(d, calculateScore(request, d)))
                .sorted(Comparator.comparingDouble(MatchCandidate::score).reversed())
                .toList();
    }

    private boolean matchesVehicle(Vehicle vehicle, ShipmentRequest request) {
        if (vehicle == null) return false;
        if (vehicle.getVehicleType() != request.getRequiredVehicleType()) return false;
        return vehicle.getCapacityKg() >= request.getTotalCargoWeightKg();
    }

    private double calculateScore(ShipmentRequest request, Driver driver) {
        double score = BASE_SCORE;
        boolean hasMatchingPreferredRoute = driver.getPreferredRoutes().stream()
                .anyMatch(r -> matchesRoute(r, request));
        if (hasMatchingPreferredRoute) {
            score += PREFERRED_ROUTE_BONUS;
        }
        return score;
    }

    private boolean matchesRoute(PreferredRoute route, ShipmentRequest request) {
        return route.getOriginRegion().equals(request.getOriginRegion())
                && route.getDestinationRegion().equals(request.getDestinationRegion());
    }
}
