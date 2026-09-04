package com.cjlogistics.mini.demo;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
@Profile("demo")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class DemoDataController {

    private final DemoDataInitializer demoDataInitializer;

    @GetMapping("/state")
    public DemoStateResponse state() {
        return demoDataInitializer.currentState();
    }
}
