package com.cjlogistics.mini.shipper;

import com.cjlogistics.mini.shipper.dto.ShipperResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shippers")
@RequiredArgsConstructor
public class ShipperController {

    private final ShipperService shipperService;

    @GetMapping("/{id}")
    public ShipperResponse get(@PathVariable Long id) {
        return ShipperResponse.from(shipperService.get(id));
    }
}
