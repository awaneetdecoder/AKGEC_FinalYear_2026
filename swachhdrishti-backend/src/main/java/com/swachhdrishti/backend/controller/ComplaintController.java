package com.swachhdrishti.backend.controller;

import com.swachhdrishti.backend.dto.ComplaintResponse;
import com.swachhdrishti.backend.dto.ComplaintSubmitRequest;
import com.swachhdrishti.backend.entity.Complaint;
import com.swachhdrishti.backend.exception.ApiException;
import com.swachhdrishti.backend.security.RequestUser;
import com.swachhdrishti.backend.service.ComplaintService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    // Citizen app: submit a geotagged photo. Runs the AI + severity pipeline.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse submit(@Valid @RequestBody ComplaintSubmitRequest request,
                                    HttpServletRequest httpRequest) {
        RequestUser caller = RequestUser.from(httpRequest).requireRole("CITIZEN");
        return ComplaintResponse.from(complaintService.submit(caller.userId(), request));
    }

    // Citizen app: my own reports, newest first.
    @GetMapping("/mine")
    public List<ComplaintResponse> mine(HttpServletRequest httpRequest) {
        RequestUser caller = RequestUser.from(httpRequest).requireRole("CITIZEN");
        return complaintService.getMine(caller.userId()).stream().map(ComplaintResponse::from).toList();
    }

    // Staff portal: the priority queue. Optional filter: GET /api/complaints?status=PENDING
    @GetMapping
    public List<ComplaintResponse> list(@RequestParam(required = false) Complaint.Status status,
                                        HttpServletRequest httpRequest) {
        RequestUser.from(httpRequest).requireRole("WORKER", "OFFICER");
        return complaintService.getQueue(status).stream().map(ComplaintResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ComplaintResponse getOne(@PathVariable Long id, HttpServletRequest httpRequest) {
        RequestUser caller = RequestUser.from(httpRequest);
        Complaint complaint = complaintService.getById(id);

        // Citizens may only look at their own reports.
        if ("CITIZEN".equals(caller.role())
                && !complaint.getCitizen().getUserId().equals(caller.userId())) {
            throw new ApiException(403, "You can only view your own complaints.");
        }
        return ComplaintResponse.from(complaint);
    }
}
