package com.cjlogistics.mini.driver;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverService {

    private final DriverRepository driverRepository;

    @Transactional
    public Driver create(
            String name,
            String phone,
            VehicleType vehicleType,
            Integer capacityKg,
            List<PreferredRoute> preferredRoutes
    ) {
        Vehicle vehicle = new Vehicle(vehicleType, capacityKg);
        Driver driver = new Driver(name, phone, vehicle);
        preferredRoutes.forEach(driver::addPreferredRoute);
        return driverRepository.save(driver);
    }

    public Driver get(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException(id));
    }
}
