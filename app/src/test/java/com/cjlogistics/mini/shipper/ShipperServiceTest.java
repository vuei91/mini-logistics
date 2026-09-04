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

@ExtendWith(MockitoExtension.class)
class ShipperServiceTest {

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
