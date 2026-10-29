package com.swachhdrishti.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Unauthenticated, non-sensitive info the Staff Portal needs before login. */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    @Value("${roboflow.mock:true}")
    private boolean roboflowMock;

    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("demoMode", demoMode, "aiMock", roboflowMock);
    }
}
