package com.swachhdrishti.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// This is the file that starts your whole backend. Run THIS file (the
// green ▶ button next to "main") to start the server.
//
// @EnableScheduling turns on the two background jobs: the 72-hour fraud
// check and the weekly K-Means hotspot prediction. Without this
// annotation, those @Scheduled methods would just sit there and never run.
@SpringBootApplication
@EnableScheduling
public class SwachhDrishtiBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(SwachhDrishtiBackendApplication.class, args);
    }
}
