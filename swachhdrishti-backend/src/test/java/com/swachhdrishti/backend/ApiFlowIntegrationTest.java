package com.swachhdrishti.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
        "staff.access-code=test-code",
        "app.upload-dir=target/test-uploads",
        "app.hotspot.initial-delay-ms=3600000",      // keep background jobs out of the way;
        "app.repeat-monitor.initial-delay-ms=3600000" // tests trigger them explicitly
})
class ApiFlowIntegrationTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    // ------------------------------------------------------------------ helpers

    private static final String PASSWORD = "Str0ng-pass-2026";

    /** Creates an account, then signs in with the same details; returns the sign-in token. */
    private String signUpAndLogin(String email, String role, String accessCode) throws Exception {
        String reg = mapper.writeValueAsString(Map.of(
                "name", "Test " + role, "email", email, "password", PASSWORD,
                "role", role, "accessCode", accessCode));
        mvc.perform(post("/api/auth/register").contentType(JSON).content(reg))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value(role));

        String login = mapper.writeValueAsString(Map.of("email", email, "password", PASSWORD));
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(JSON).content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(role)).andReturn();
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
    }

    @Test
    void corsPreflightIsNotBlockedByTheAuthFilter() throws Exception {
        mvc.perform(options("/api/complaints")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    void staffCannotRegisterWithoutTheRegistrationCode() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "name", "Sneaky", "email", "sneaky@example.com", "password", PASSWORD,
                "role", "OFFICER", "accessCode", "guess"));
        mvc.perform(post("/api/auth/register").contentType(JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateEmailAndWrongPasswordAreRejected() throws Exception {
        signUpAndLogin("dup@example.com", "CITIZEN", "");

        String again = mapper.writeValueAsString(Map.of(
                "name", "Dup", "email", "DUP@example.com", "password", PASSWORD,
                "role", "CITIZEN", "accessCode", ""));
        mvc.perform(post("/api/auth/register").contentType(JSON).content(again))
                .andExpect(status().isConflict());

        String wrong = mapper.writeValueAsString(Map.of("email", "dup@example.com", "password", "not-the-password"));
        mvc.perform(post("/api/auth/login").contentType(JSON).content(wrong))
                .andExpect(status().isUnauthorized());

        String unknown = mapper.writeValueAsString(Map.of("email", "nobody@example.com", "password", PASSWORD));
        mvc.perform(post("/api/auth/login").contentType(JSON).content(unknown))
                .andExpect(status().isUnauthorized());

        String shortPw = mapper.writeValueAsString(Map.of(
                "name", "Short", "email", "short@example.com", "password", "abc",
                "role", "CITIZEN", "accessCode", ""));
        mvc.perform(post("/api/auth/register").contentType(JSON).content(shortPw))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rolesAreEnforced() throws Exception {
        String citizen = signUpAndLogin("roles-citizen@example.com", "CITIZEN", "");
        String worker = signUpAndLogin("roles-worker@example.com", "WORKER", "test-code");

        mvc.perform(get("/api/officer/stats").header("Authorization", bearer(citizen))).andExpect(status().isForbidden());
        mvc.perform(get("/api/officer/stats").header("Authorization", bearer(worker))).andExpect(status().isForbidden());
        mvc.perform(get("/api/complaints").header("Authorization", bearer(citizen))).andExpect(status().isForbidden());
        mvc.perform(get("/api/complaints").header("Authorization", bearer(worker))).andExpect(status().isOk());
    }

    @Test
    void inputValidationReturnsCleanJsonErrors() throws Exception {
        String citizen = signUpAndLogin("valid-citizen@example.com", "CITIZEN", "");
        String worker = signUpAndLogin("valid-worker@example.com", "WORKER", "test-code");

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

    @Test
    void photoUploadAndDownloadRoundTrip() throws Exception {
        String citizen = signUpAndLogin("photo-citizen@example.com", "CITIZEN", "");
        byte[] bytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};
        MockMultipartFile photo = new MockMultipartFile("file", "pile.jpg", "image/jpeg", bytes);

        MvcResult r = mvc.perform(multipart("/api/uploads").file(photo).header("Authorization", bearer(citizen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").isString())
                .andReturn();
        String url = mapper.readTree(r.getResponse().getContentAsString()).get("url").asText();
        String path = url.substring(url.indexOf("/uploads/"));

        // The uploaded file can be fetched back (no token needed, like an <img> tag).
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().bytes(bytes));

        // Wrong file type and missing token are refused; unknown or malicious names give 404.
        MockMultipartFile text = new MockMultipartFile("file", "x.txt", "text/plain", "hello".getBytes());
        mvc.perform(multipart("/api/uploads").file(text).header("Authorization", bearer(citizen)))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/uploads").file(photo)).andExpect(status().isUnauthorized());
        mvc.perform(get("/uploads/not-a-real-photo.jpg")).andExpect(status().isNotFound());
    }

    /** The paper's full story: report -> reject far worker -> accept near worker -> reward -> repeat trap. */
    @Test
    void fullAccountabilityFlow() throws Exception {
        String citizen = signUpAndLogin("flow-citizen@example.com", "CITIZEN", "");
        String worker = signUpAndLogin("flow-worker@example.com", "WORKER", "test-code");
        String officer = signUpAndLogin("flow-officer@example.com", "OFFICER", "test-code");

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
