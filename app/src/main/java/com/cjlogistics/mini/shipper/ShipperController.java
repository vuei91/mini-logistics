package com.cjlogistics.mini.shipper;

import com.cjlogistics.mini.shipper.dto.ShipperCreateRequest;
import com.cjlogistics.mini.shipper.dto.ShipperResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/shippers")
@RequiredArgsConstructor
public class ShipperController {

    private final ShipperService shipperService;

    @PostMapping
    public ResponseEntity<ShipperResponse> create(@Valid @RequestBody ShipperCreateRequest request) {
        Shipper shipper = shipperService.create(request.name(), request.phone());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(shipper.getId())
                .toUri();
        return ResponseEntity.created(location).body(ShipperResponse.from(shipper));
    }

    @GetMapping("/{id}")
    public ShipperResponse get(@PathVariable Long id) {
        return ShipperResponse.from(shipperService.get(id));
    }
}
