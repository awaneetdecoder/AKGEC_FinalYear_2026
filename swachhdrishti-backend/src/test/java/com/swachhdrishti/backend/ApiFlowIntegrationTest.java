package com.swachhdrishti.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the whole application on in-memory H2 and drives the REST API the
 * way the Flutter app and the Staff Portal do.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:itest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "app.demo-mode=false",                       // start from an empty database
        "staff.access-code=test-code",
        "app.hotspot.initial-delay-ms=3600000",      // keep background jobs out of the way;
        "app.repeat-monitor.initial-delay-ms=3600000" // tests trigger them explicitly
})
class ApiFlowIntegrationTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    // ------------------------------------------------------------------ helpers

    private String login(String email, String role, String accessCode) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "email", email, "name", "Test " + role, "role", role, "accessCode", accessCode));
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private long submitComplaint(String citizenToken, double lat, double lng) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "photoUrl", "https://example.com/photo-" + System.nanoTime() + ".jpg",
                "latitude", lat, "longitude", lng));
        MvcResult r = mvc.perform(post("/api/complaints").header("Authorization", bearer(citizenToken))
                        .contentType(JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.severity").isNumber())
                .andReturn();
        JsonNode json = mapper.readTree(r.getResponse().getContentAsString());
        return json.get("complaintId").asLong();
    }

    private String verifyBody(long complaintId, double lat, double lng) throws Exception {
        return mapper.writeValueAsString(Map.of(
                "complaintId", complaintId, "workerLatitude", lat, "workerLongitude", lng));
    }

    // -------------------------------------------------------------------- tests

    @Test
    void protectedEndpointsNeedAToken() throws Exception {
        mvc.perform(get("/api/complaints")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/complaints").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/public/config")).andExpect(status().isOk());
    }

    @Test
    void corsPreflightIsNotBlockedByTheAuthFilter() throws Exception {
        mvc.perform(options("/api/complaints")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    void staffCannotSelfDeclareWithoutTheAccessCode() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "email", "sneaky@example.com", "name", "Sneaky", "role", "OFFICER", "accessCode", "guess"));
        mvc.perform(post("/api/auth/login").contentType(JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolesAreEnforced() throws Exception {
        String citizen = login("roles-citizen@example.com", "CITIZEN", "");
        String worker = login("roles-worker@example.com", "WORKER", "test-code");

        mvc.perform(get("/api/officer/stats").header("Authorization", bearer(citizen))).andExpect(status().isForbidden());
        mvc.perform(get("/api/officer/stats").header("Authorization", bearer(worker))).andExpect(status().isForbidden());
        mvc.perform(get("/api/complaints").header("Authorization", bearer(citizen))).andExpect(status().isForbidden());
        mvc.perform(get("/api/complaints").header("Authorization", bearer(worker))).andExpect(status().isOk());
    }

    @Test
    void inputValidationReturnsCleanJsonErrors() throws Exception {
        String citizen = login("valid-citizen@example.com", "CITIZEN", "");
        String worker = login("valid-worker@example.com", "WORKER", "test-code");

        String badLat = mapper.writeValueAsString(Map.of(
                "photoUrl", "https://example.com/a.jpg", "latitude", 123.0, "longitude", 77.0));
        mvc.perform(post("/api/complaints").header("Authorization", bearer(citizen)).contentType(JSON).content(badLat))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        String badUrl = mapper.writeValueAsString(Map.of(
                "photoUrl", "not-a-url", "latitude", 28.0, "longitude", 77.0));
        mvc.perform(post("/api/complaints").header("Authorization", bearer(citizen)).contentType(JSON).content(badUrl))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/complaints?status=BANANA").header("Authorization", bearer(worker)))
                .andExpect(status().isBadRequest());
    }

    /** The paper's full story: report -> reject far worker -> accept near worker -> reward -> repeat trap. */
    @Test
    void fullAccountabilityFlow() throws Exception {
        String citizen = login("flow-citizen@example.com", "CITIZEN", "");
        String worker = login("flow-worker@example.com", "WORKER", "test-code");
        String officer = login("flow-officer@example.com", "OFFICER", "test-code");

        // 1. Citizen reports garbage; the AI pipeline assigns a severity and SLA.
        double spotLat = 28.5000, spotLng = 77.2000;
        long id = submitComplaint(citizen, spotLat, spotLng);

        // 2. Worker claims it.
        mvc.perform(post("/api/worker/complaints/" + id + "/assign").header("Authorization", bearer(worker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        // 3. Worker tries to confirm from far away from any landfill -> rejected by the geofence.
        mvc.perform(post("/api/worker/verify").header("Authorization", bearer(worker))
                        .contentType(JSON).content(verifyBody(id, 28.7000, 77.5000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(false));

        // 4. Same worker, ~22 m from the seeded "Sector 12 Landfill" -> accepted.
        mvc.perform(post("/api/worker/verify").header("Authorization", bearer(worker))
                        .contentType(JSON).content(verifyBody(id, 28.6692 + 0.0002, 77.4538)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        mvc.perform(get("/api/complaints/" + id).header("Authorization", bearer(worker)))
                .andExpect(jsonPath("$.status").value("CLEANED"));

        // 5. Verifying again is refused; coins are paid exactly once.
        mvc.perform(post("/api/worker/verify").header("Authorization", bearer(worker))
                        .contentType(JSON).content(verifyBody(id, 28.6692, 77.4538)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(citizen)))
                .andExpect(jsonPath("$.cleanCoins").value(10))
                .andExpect(jsonPath("$.totalReports").value(1));

        // 6. The officer sees the rejected attempt in the evidence log.
        mvc.perform(get("/api/officer/disposal-logs?authorised=false").header("Authorization", bearer(officer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorised").value(false));

        // 7. A new complaint ~22 m from the "cleaned" spot -> the repeat trap reopens the ticket.
        Thread.sleep(50); // make sure the new complaint is strictly after the closure time
        submitComplaint(citizen, spotLat + 0.0002, spotLng);

        mvc.perform(post("/api/officer/repeat-monitor/run").header("Authorization", bearer(officer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketsReopened").value(1));

        mvc.perform(get("/api/complaints/" + id).header("Authorization", bearer(officer)))
                .andExpect(jsonPath("$.status").value("REOPENED"));

        // 8. Reopened tickets float to the top of the priority queue.
        MvcResult queue = mvc.perform(get("/api/complaints").header("Authorization", bearer(officer)))
                .andExpect(status().isOk()).andReturn();
        assertEquals(id, mapper.readTree(queue.getResponse().getContentAsString()).get(0).get("complaintId").asLong());

        // 9. Dashboard counters reflect it.
        mvc.perform(get("/api/officer/stats").header("Authorization", bearer(officer)))
                .andExpect(jsonPath("$.reopened").value(1))
                .andExpect(jsonPath("$.rejectedCleanupAttempts").value(1));

        // 10. Hotspot job can be run on demand (too little data -> 0 clusters, but no error).
        mvc.perform(post("/api/officer/hotspots/refresh").header("Authorization", bearer(officer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clustersWritten").isNumber());
    }
}
