package com.swachhdrishti.backend.service;

import com.swachhdrishti.backend.entity.Complaint;
import com.swachhdrishti.backend.entity.DisposalZone;
import com.swachhdrishti.backend.entity.User;
import com.swachhdrishti.backend.repository.ComplaintRepository;
import com.swachhdrishti.backend.repository.DisposalZoneRepository;
import com.swachhdrishti.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Runs once at start-up and only adds data that is missing, so restarting is
 * always safe.
 *
 *  1. Always: the authorised disposal zones the worker GPS check needs.
 *  2. Only when app.demo-mode=true: three demo accounts and ~40 sample
 *     complaints in five clusters, so the dashboards and the K-Means
 *     hotspot view have something to show straight away.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    // {latitude, longitude, number of complaints} - five areas around Ghaziabad.
    private static final double[][] DEMO_CLUSTERS = {
            {28.6692, 77.4538, 14},
            {28.6551, 77.3910, 9},
            {28.6870, 77.4210, 5},
            {28.6405, 77.4100, 12},
            {28.7010, 77.4500, 3},
    };

    private final DisposalZoneRepository disposalZoneRepository;
    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final SeverityService severityService;
    private final HotspotSchedulerService hotspotSchedulerService;

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    public DataSeeder(DisposalZoneRepository disposalZoneRepository,
                      UserRepository userRepository,
                      ComplaintRepository complaintRepository,
                      SeverityService severityService,
                      HotspotSchedulerService hotspotSchedulerService) {
        this.disposalZoneRepository = disposalZoneRepository;
        this.userRepository = userRepository;
        this.complaintRepository = complaintRepository;
        this.severityService = severityService;
        this.hotspotSchedulerService = hotspotSchedulerService;
    }

    @Override
    public void run(String... args) {
        seedDisposalZones();
        if (demoMode) {
            seedDemoData();
        }
    }

    private void seedDisposalZones() {
        if (disposalZoneRepository.count() > 0) return;

        disposalZoneRepository.save(zone("Sector 12 Landfill", 28.6692, 77.4538, 50));
        disposalZoneRepository.save(zone("Vasundhara Transfer Station", 28.6551, 77.3910, 50));
        log.info("SwachhDrishti: seeded 2 default disposal zones.");
    }

    private DisposalZone zone(String name, double lat, double lng, int radius) {
        DisposalZone z = new DisposalZone();
        z.setZoneName(name);
        z.setLatitude(lat);
        z.setLongitude(lng);
        z.setRadiusMeters(radius);
        z.setIsActive(true);
        return z;
    }

    private void seedDemoData() {
        User citizen = ensureUser("citizen@swachh.demo", "Demo Citizen", User.Role.CITIZEN);
        ensureUser("worker@swachh.demo", "Demo Worker", User.Role.WORKER);
        ensureUser("officer@swachh.demo", "Demo Officer", User.Role.OFFICER);

        if (complaintRepository.count() > 0) return;

        Random random = new Random(2026); // fixed seed = reproducible demo
        String[] types = {"Plastic", "Organic", "Construction Debris", "Mixed", "E-Waste"};
        int n = 0;

        for (double[] cluster : DEMO_CLUSTERS) {
            for (int i = 0; i < (int) cluster[2]; i++) {
                n++;
                double areaPct = 1 + random.nextInt(70);
                int severity = severityService.calculateSeverity(areaPct);

                Complaint c = new Complaint();
                c.setCitizen(citizen);
                c.setPhotoUrl("https://placehold.co/600x400?text=Garbage+" + n);
                // ~ +/- 150 m of scatter around the cluster centre
                c.setLatitude(cluster[0] + (random.nextDouble() - 0.5) * 0.003);
                c.setLongitude(cluster[1] + (random.nextDouble() - 0.5) * 0.003);
                c.setGarbageType(types[random.nextInt(types.length)]);
                c.setBoundingBoxAreaPct(areaPct);
                c.setSeverity(severity);
                c.setSlaDeadlineHours(severityService.calculateSlaHours(severity));
                c.setStatus(Complaint.Status.PENDING);
                c.setCreatedAt(LocalDateTime.now().minusHours(random.nextInt(24 * 20)));
                complaintRepository.save(c);
            }
        }

        citizen.setTotalReports(n);
        userRepository.save(citizen);

        int clusters = hotspotSchedulerService.generateHotspots();
        log.info("SwachhDrishti DEMO MODE: seeded 3 demo users, {} complaints, {} hotspot clusters.", n, clusters);
    }

    private User ensureUser(String email, String name, User.Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setName(name);
            u.setRole(role);
            return userRepository.save(u);
        });
    }
}
