package com.cjlogistics.mini.driver.dto;

import com.cjlogistics.mini.driver.Driver;
import com.cjlogistics.mini.driver.DriverStatus;
import com.cjlogistics.mini.driver.PreferredRoute;
import com.cjlogistics.mini.driver.Vehicle;
import com.cjlogistics.mini.driver.VehicleType;

import java.util.List;

public record DriverResponse(
        Long id,
        String name,
        String phone,
        DriverStatus status,
        VehicleData vehicle,
        List<RouteData> preferredRoutes
) {
    public record VehicleData(Long id, VehicleType vehicleType, Integer capacityKg) {
        public static VehicleData from(Vehicle v) {
            return new VehicleData(v.getId(), v.getVehicleType(), v.getCapacityKg());
        }
    }

    public record RouteData(String originRegion, String destinationRegion) {
        public static RouteData from(PreferredRoute r) {
            return new RouteData(r.getOriginRegion(), r.getDestinationRegion());
        }
    }

    public static DriverResponse from(Driver d) {
        List<RouteData> routes = d.getPreferredRoutes().stream()
                .map(RouteData::from)
                .toList();
        return new DriverResponse(
                d.getId(),
                d.getName(),
                d.getPhone(),
                d.getStatus(),
                VehicleData.from(d.getVehicle()),
                routes
        );
    }
}
