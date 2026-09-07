package com.cjlogistics.mini.dispatch.dto;

import com.cjlogistics.mini.dispatch.MatchCandidate;
import com.cjlogistics.mini.driver.Driver;
import com.cjlogistics.mini.driver.DriverStatus;
import com.cjlogistics.mini.driver.PreferredRoute;
import com.cjlogistics.mini.driver.Vehicle;
import com.cjlogistics.mini.driver.VehicleType;

import java.util.List;

/**
 * 화주에게 노출되는 매칭 후보 기사 정보 + 점수.
 * 배차를 생성하지 않고 조회만 하는 용도.
 */
public record MatchCandidateResponse(
        Long driverId,
        String name,
        DriverStatus status,
        VehicleData vehicle,
        List<RouteData> preferredRoutes,
        double matchScore,
        java.math.BigDecimal estimatedFare
) {
    public record VehicleData(VehicleType vehicleType, Integer capacityKg) {
        public static VehicleData from(Vehicle v) {
            return new VehicleData(v.getVehicleType(), v.getCapacityKg());
        }
    }

    public record RouteData(String originRegion, String destinationRegion) {
        public static RouteData from(PreferredRoute r) {
            return new RouteData(r.getOriginRegion(), r.getDestinationRegion());
        }
    }

    public static MatchCandidateResponse from(MatchCandidate candidate, java.math.BigDecimal estimatedFare) {
        Driver d = candidate.driver();
        return new MatchCandidateResponse(
                d.getId(),
                d.getName(),
                d.getStatus(),
                VehicleData.from(d.getVehicle()),
                d.getPreferredRoutes().stream().map(RouteData::from).toList(),
                candidate.score(),
                estimatedFare
        );
    }
}
