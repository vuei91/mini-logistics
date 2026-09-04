package com.cjlogistics.mini.shipper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ShipperServiceTest {

    @Test
    void register_normalizes_login_email_and_keeps_only_password_hash() {
        Shipper shipper = Shipper.register("CJ 화주", "010-1111-2222", "CJ@Example.COM", "bcrypt-hash");

        assertThat(shipper.getEmail()).isEqualTo("cj@example.com");
        assertThat(shipper.getPasswordHash()).isEqualTo("bcrypt-hash");
    }

    @Mock
    ShipperRepository shipperRepository;

    @InjectMocks
    ShipperService shipperService;

    @Test
    void create_persists_and_returns_shipper() {
        given(shipperRepository.save(any(Shipper.class)))
                .willAnswer(inv -> inv.getArgument(0));

        Shipper result = shipperService.create("홍길동", "010-1234-5678");

        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getPhone()).isEqualTo("010-1234-5678");
    }

    @Test
    void signup_hashes_the_password_before_persisting() {
        ShipperService service = new ShipperService(shipperRepository, new BCryptPasswordEncoder());
        given(shipperRepository.save(any(Shipper.class))).willAnswer(inv -> inv.getArgument(0));

        Shipper result = service.signup("CJ 화주", "010-1234-5678", "cj@example.com", "plain-password");

        assertThat(result.getPasswordHash()).isNotEqualTo("plain-password");
        assertThat(new BCryptPasswordEncoder().matches("plain-password", result.getPasswordHash())).isTrue();
    }

    @Test
    void signup_rejects_an_already_registered_email() {
        ShipperService service = new ShipperService(shipperRepository, new BCryptPasswordEncoder());
        given(shipperRepository.existsByEmail("cj@example.com")).willReturn(true);

        assertThatThrownBy(() -> service.signup("CJ 화주", "010-1234-5678", "CJ@Example.COM", "plain-password"))
                .isInstanceOf(DuplicateShipperEmailException.class);

        verify(shipperRepository).existsByEmail("cj@example.com");
    }

    @Test
    void get_returns_shipper_when_exists() {
        Shipper shipper = new Shipper("홍길동", "010-1234-5678");
        given(shipperRepository.findById(1L)).willReturn(Optional.of(shipper));

        Shipper result = shipperService.get(1L);

        assertThat(result.getName()).isEqualTo("홍길동");
    }

    @Test
    void get_throws_when_not_found() {
        given(shipperRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> shipperService.get(999L))
                .isInstanceOf(ShipperNotFoundException.class);
    }
}
