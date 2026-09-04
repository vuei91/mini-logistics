package com.cjlogistics.mini.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataController {

    private final DemoDataInitializer demoDataInitializer;

    @GetMapping("/state")
    public DemoStateResponse state() {
        return demoDataInitializer.currentState();
    }
}
