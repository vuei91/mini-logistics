package com.cjlogistics.mini.driver;

import com.cjlogistics.mini.driver.dto.DriverResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping("/{id}")
    public DriverResponse get(@PathVariable Long id) {
        return DriverResponse.from(driverService.get(id));
    }
}
