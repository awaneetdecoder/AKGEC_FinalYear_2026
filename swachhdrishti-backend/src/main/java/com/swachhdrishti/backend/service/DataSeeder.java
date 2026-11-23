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
 * Runs once at start-up and only adds data that is missing, so restarting is safe.
 *
 *  1. Always: the authorised disposal zones the worker GPS check needs.
 *  2. Only when app.seed-sample-data=true (env SEED_SAMPLE_DATA): ~40 placeholder
 *     complaints in five clusters so the hotspot (K-Means) view has data to show.
 *     They are test data, not real citizen reports. Off by default.
 *
 * No user accounts are ever created here: everyone signs up themselves.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    // {latitude, longitude, number of complaints}
    private static final double[][] SAMPLE_CLUSTERS = {
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

    @Value("${app.seed-sample-data:false}")
    private boolean seedSampleData;

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
        if (seedSampleData) {
            seedSampleComplaints();
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

    private void seedSampleComplaints() {
        if (complaintRepository.count() > 0) return;

        // A placeholder owner without a password: nobody can sign in as it.
        User owner = userRepository.findByEmail("sample-data@swachhdrishti.invalid").orElseGet(() -> {
            User u = new User();
            u.setEmail("sample-data@swachhdrishti.invalid");
            u.setName("Sample data");
            u.setRole(User.Role.CITIZEN);
            return userRepository.save(u);
        });

        Random random = new Random(2026); // fixed seed = reproducible
        String[] types = {"Plastic", "Organic", "Construction Debris", "Mixed", "E-Waste"};
        int n = 0;

        for (double[] cluster : SAMPLE_CLUSTERS) {
            for (int i = 0; i < (int) cluster[2]; i++) {
                n++;
                double areaPct = 1 + random.nextInt(70);
                int severity = severityService.calculateSeverity(areaPct);

                Complaint c = new Complaint();
                c.setCitizen(owner);
                c.setPhotoUrl("https://placehold.co/600x400?text=Sample+" + n);
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

        owner.setTotalReports(n);
        userRepository.save(owner);

        int clusters = hotspotSchedulerService.generateHotspots();
        log.info("SwachhDrishti: seeded {} sample complaints and {} hotspot clusters.", n, clusters);
    }
}
