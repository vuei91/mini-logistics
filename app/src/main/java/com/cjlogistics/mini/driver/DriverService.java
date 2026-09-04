package com.cjlogistics.mini.driver;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Locale;
import com.cjlogistics.mini.auth.InvalidCredentialsException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverService {

    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;

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

    @Transactional
    public Driver signup(
            String name,
            String phone,
            String email,
            String password,
            VehicleType vehicleType,
            Integer capacityKg,
            List<PreferredRoute> preferredRoutes
    ) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (driverRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateDriverEmailException(normalizedEmail);
        }
        Driver driver = Driver.register(
                name, phone, normalizedEmail, passwordEncoder.encode(password), new Vehicle(vehicleType, capacityKg));
        preferredRoutes.forEach(driver::addPreferredRoute);
        return driverRepository.save(driver);
    }

    public Driver login(String email, String password) {
        Driver driver = driverRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, driver.getPasswordHash())) throw new InvalidCredentialsException();
        return driver;
    }

    public Driver get(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException(id));
    }
}
