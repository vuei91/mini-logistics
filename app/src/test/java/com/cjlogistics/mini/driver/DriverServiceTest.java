package com.cjlogistics.mini.driver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Test
    void register_normalizes_login_email_and_keeps_only_password_hash() {
        Driver driver = Driver.register(
                "김운전", "010-3333-4444", "DRIVER@Example.COM", "bcrypt-hash",
                new Vehicle(VehicleType.TRUCK_1T, 1000));

        assertThat(driver.getEmail()).isEqualTo("driver@example.com");
        assertThat(driver.getPasswordHash()).isEqualTo("bcrypt-hash");
    }

    @Mock
    DriverRepository driverRepository;

    @InjectMocks
    DriverService driverService;

    @Test
    void create_persists_driver_with_vehicle_and_routes() {
        given(driverRepository.save(any(Driver.class)))
                .willAnswer(inv -> inv.getArgument(0));

        Driver result = driverService.create(
                "김운전", "010-2222-3333",
                VehicleType.TRUCK_2_5T, 2500,
                List.of(new PreferredRoute("서울", "부산"))
        );

        assertThat(result.getName()).isEqualTo("김운전");
        assertThat(result.getStatus()).isEqualTo(DriverStatus.AVAILABLE);
        assertThat(result.getVehicle()).isNotNull();
        assertThat(result.getVehicle().getVehicleType()).isEqualTo(VehicleType.TRUCK_2_5T);
        assertThat(result.getVehicle().getCapacityKg()).isEqualTo(2500);
        assertThat(result.getPreferredRoutes()).hasSize(1);
        assertThat(result.getPreferredRoutes().get(0).getOriginRegion()).isEqualTo("서울");
        assertThat(result.getPreferredRoutes().get(0).getDestinationRegion()).isEqualTo("부산");
    }

    @Test
    void signup_hashes_the_password_before_persisting() {
        DriverService service = new DriverService(driverRepository, new BCryptPasswordEncoder());
        given(driverRepository.save(any(Driver.class))).willAnswer(inv -> inv.getArgument(0));

        Driver result = service.signup(
                "김운전", "010-3333-4444", "driver@example.com", "plain-password",
                VehicleType.TRUCK_1T, 1000, List.of());

        assertThat(result.getPasswordHash()).isNotEqualTo("plain-password");
        assertThat(new BCryptPasswordEncoder().matches("plain-password", result.getPasswordHash())).isTrue();
    }

    @Test
    void signup_rejects_an_already_registered_email() {
        DriverService service = new DriverService(driverRepository, new BCryptPasswordEncoder());
        given(driverRepository.existsByEmail("driver@example.com")).willReturn(true);

        assertThatThrownBy(() -> service.signup(
                "김운전", "010-3333-4444", "DRIVER@Example.COM", "plain-password",
                VehicleType.TRUCK_1T, 1000, List.of()))
                .isInstanceOf(DuplicateDriverEmailException.class);

        verify(driverRepository).existsByEmail("driver@example.com");
    }

    @Test
    void create_with_empty_routes_works() {
        given(driverRepository.save(any(Driver.class)))
                .willAnswer(inv -> inv.getArgument(0));

        Driver result = driverService.create(
                "이운전", "010-3333-4444",
                VehicleType.TRUCK_1T, 1000,
                List.of()
        );

        assertThat(result.getPreferredRoutes()).isEmpty();
    }

    @Test
    void get_returns_driver_when_exists() {
        Vehicle vehicle = new Vehicle(VehicleType.TRUCK_5T, 5000);
        Driver driver = new Driver("박운전", "010-4444-5555", vehicle);
        given(driverRepository.findById(1L)).willReturn(Optional.of(driver));

        Driver result = driverService.get(1L);

        assertThat(result.getName()).isEqualTo("박운전");
    }

    @Test
    void get_throws_when_not_found() {
        given(driverRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.get(999L))
                .isInstanceOf(DriverNotFoundException.class);
    }
}
