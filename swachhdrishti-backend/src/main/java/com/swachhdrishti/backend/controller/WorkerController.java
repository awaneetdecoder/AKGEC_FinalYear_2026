package com.swachhdrishti.backend.controller;

import com.swachhdrishti.backend.dto.ComplaintResponse;
import com.swachhdrishti.backend.dto.WorkerVerifyRequest;
import com.swachhdrishti.backend.dto.WorkerVerifyResponse;
import com.swachhdrishti.backend.security.RequestUser;
import com.swachhdrishti.backend.service.WorkerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worker")
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    // "Claim this job": PENDING / REOPENED -> ASSIGNED.
    @PostMapping("/complaints/{id}/assign")
    public ComplaintResponse assign(@PathVariable Long id, HttpServletRequest httpRequest) {
        RequestUser caller = RequestUser.from(httpRequest).requireRole("WORKER");
        return ComplaintResponse.from(workerService.assign(caller.userId(), id));
    }

    // "Mark as Cleaned": server-side Haversine geofence check against authorised zones.
    @PostMapping("/verify")
    public WorkerVerifyResponse verify(@Valid @RequestBody WorkerVerifyRequest request,
                                       HttpServletRequest httpRequest) {
        RequestUser caller = RequestUser.from(httpRequest).requireRole("WORKER");
        return workerService.verifyCleanup(caller.userId(), request);
    }
}
