package com.swachhdrishti.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classifies a garbage photo with the YOLOv11 model hosted on Roboflow.
 *
 * Real mode (roboflow.mock=false) calls the Roboflow hosted-detection API:
 * <pre>POST {model-url}?api_key=...&amp;image={photo-url}</pre>
 * and turns the returned boxes into (dominant garbage class, coverage %).
 * Coverage = area of the UNION of all confident boxes / image area.
 *
 * Failure policy from the paper: a 3-second timeout, and on any error,
 * timeout, or when no detection reaches the confidence threshold, return the
 * fallback (Mixed, coverage 20 % -> severity 3) flagged as not confident so a
 * human reviews it. Complaint submission is never blocked by the AI.
 *
 * Mock mode (default) returns deterministic fake detections so the full
 * pipeline works with no Roboflow account.
 */
@Service
public class RoboflowService {

    private static final Logger log = LoggerFactory.getLogger(RoboflowService.class);

    /** Coverage used by the fallback; falls inside the severity-3 band (15-35 %). */
    static final double FALLBACK_COVERAGE_PCT = 20.0;

    @Value("${roboflow.mock:true}")
    private boolean mockMode;

    @Value("${roboflow.api.key:}")
    private String apiKey;

    @Value("${roboflow.model.url:}")
    private String modelUrl;

    @Value("${roboflow.confidence-threshold:0.495}")
    private double confidenceThreshold;

    private final RestTemplate restTemplate;

    public RoboflowService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * @param garbageType        dominant waste class
     * @param boundingBoxAreaPct garbage coverage of the frame, 0-100
     * @param confident          false when the fallback was used (needs human review)
     */
    public record DetectionResult(String garbageType, double boundingBoxAreaPct, boolean confident) {
        static DetectionResult fallback() {
            return new DetectionResult("Mixed", FALLBACK_COVERAGE_PCT, false);
        }
    }

    public DetectionResult classify(String photoUrl) {
        if (mockMode) {
            return mockDetection(photoUrl);
        }
        if (modelUrl == null || modelUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            log.warn("roboflow.mock=false but ROBOFLOW_API_KEY / ROBOFLOW_MODEL_URL are not set - using fallback.");
            return DetectionResult.fallback();
        }

        try {
            URI uri = UriComponentsBuilder.fromUriString(modelUrl)
                    .queryParam("api_key", "{key}")
                    .queryParam("image", "{image}")
                    .encode()
                    .buildAndExpand(Map.of("key", apiKey, "image", photoUrl))
                    .toUri();

            JsonNode response = restTemplate.postForObject(uri, null, JsonNode.class);
            return parseResponse(response, confidenceThreshold);

        } catch (Exception e) {
            // Timeout, network error, bad status, bad JSON...
            log.warn("Roboflow call failed ({}), using fallback classification.", e.toString());
            return DetectionResult.fallback();
        }
    }

    /** Package-private + static so it can be unit-tested without HTTP. */
    static DetectionResult parseResponse(JsonNode root, double threshold) {
        if (root == null) return DetectionResult.fallback();

        double imgW = root.path("image").path("width").asDouble(0);
        double imgH = root.path("image").path("height").asDouble(0);
        if (imgW <= 0 || imgH <= 0) return DetectionResult.fallback();

        List<BoxGeometry.Box> boxes = new ArrayList<>();
        Map<String, Double> areaByClass = new HashMap<>();

        for (JsonNode p : root.path("predictions")) {
            if (p.path("confidence").asDouble(0) < threshold) continue;

            // Roboflow boxes are centre-based: x, y = centre; width, height.
            double cx = p.path("x").asDouble();
            double cy = p.path("y").asDouble();
            double w = p.path("width").asDouble();
            double h = p.path("height").asDouble();

            BoxGeometry.Box box = new BoxGeometry.Box(
                    Math.max(0, cx - w / 2), Math.max(0, cy - h / 2),
                    Math.min(imgW, cx + w / 2), Math.min(imgH, cy + h / 2));
            if (box.area() <= 0) continue;

            boxes.add(box);
            areaByClass.merge(p.path("class").asText("Mixed"), box.area(), Double::sum);
        }

        if (boxes.isEmpty()) return DetectionResult.fallback();

        double coveragePct = Math.min(100.0, BoxGeometry.unionArea(boxes) / (imgW * imgH) * 100.0);
        String dominant = areaByClass.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Mixed");

        return new DetectionResult(dominant, Math.round(coveragePct * 10) / 10.0, true);
    }

    // Deterministic offline stand-in (ROBOFLOW_MOCK=true): varied results so severities are not all identical.
    private DetectionResult mockDetection(String photoUrl) {
        String[] types = {"Plastic", "Organic", "Construction Debris", "Mixed", "E-Waste"};
        int hash = photoUrl == null ? 0 : photoUrl.hashCode();
        String type = types[Math.floorMod(hash, types.length)];
        double areaPct = 1 + Math.floorMod(hash / 7, 70); // spreads across all 5 severity levels
        return new DetectionResult(type, areaPct, true);
    }
}
