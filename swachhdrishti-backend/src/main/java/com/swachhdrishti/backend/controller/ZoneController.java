package com.swachhdrishti.backend.controller;

import com.swachhdrishti.backend.dto.ZoneResponse;
import com.swachhdrishti.backend.repository.DisposalZoneRepository;
import com.swachhdrishti.backend.security.RequestUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The authorised disposal zones (read-only), so staff can see where cleanups can be confirmed. */
@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final DisposalZoneRepository disposalZoneRepository;

    public ZoneController(DisposalZoneRepository disposalZoneRepository) {
        this.disposalZoneRepository = disposalZoneRepository;
    }

    @GetMapping
    public List<ZoneResponse> list(HttpServletRequest request) {
        RequestUser.from(request).requireRole("WORKER", "OFFICER");
        return disposalZoneRepository.findByIsActiveTrue().stream().map(ZoneResponse::from).toList();
    }
}
