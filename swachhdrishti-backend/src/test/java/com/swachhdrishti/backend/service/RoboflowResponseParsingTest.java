package com.swachhdrishti.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parsing of the Roboflow hosted-detection response, without any HTTP. */
class RoboflowResponseParsingTest {

    private static final double THRESHOLD = 0.495;
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode json(String s) throws Exception {
        return mapper.readTree(s);
    }

    @Test
    void coverageIsBoxAreaOverImageArea() throws Exception {
        // 100x100 image, one 50x40 box (centre-based) -> 2000 / 10000 = 20 %
        var r = RoboflowService.parseResponse(json("""
            {"image":{"width":100,"height":100},
             "predictions":[{"x":50,"y":50,"width":50,"height":40,"confidence":0.9,"class":"Plastic"}]}
            """), THRESHOLD);

        assertTrue(r.confident());
        assertEquals("Plastic", r.garbageType());
        assertEquals(20.0, r.boundingBoxAreaPct(), 0.05);
    }

    @Test
    void overlappingBoxesAreNotDoubleCounted() throws Exception {
        // two identical 50x50 boxes on a 100x100 image: coverage 25 %, not 50 %
        var r = RoboflowService.parseResponse(json("""
            {"image":{"width":100,"height":100},"predictions":[
              {"x":50,"y":50,"width":50,"height":50,"confidence":0.8,"class":"Mixed"},
              {"x":50,"y":50,"width":50,"height":50,"confidence":0.7,"class":"Mixed"}]}
            """), THRESHOLD);

        assertEquals(25.0, r.boundingBoxAreaPct(), 0.05);
    }

    @Test
    void dominantClassIsTheOneWithMostArea() throws Exception {
        var r = RoboflowService.parseResponse(json("""
            {"image":{"width":200,"height":200},"predictions":[
              {"x":30,"y":30,"width":20,"height":20,"confidence":0.9,"class":"Glass"},
              {"x":120,"y":120,"width":80,"height":80,"confidence":0.9,"class":"Construction Debris"}]}
            """), THRESHOLD);

        assertEquals("Construction Debris", r.garbageType());
    }

    @Test
    void lowConfidenceDetectionsAreIgnored() throws Exception {
        var r = RoboflowService.parseResponse(json("""
            {"image":{"width":100,"height":100},
             "predictions":[{"x":50,"y":50,"width":90,"height":90,"confidence":0.30,"class":"Plastic"}]}
            """), THRESHOLD);

        assertFalse(r.confident());                  // -> fallback, flagged for review
        assertEquals("Mixed", r.garbageType());
        assertEquals(3, new SeverityService().calculateSeverity(r.boundingBoxAreaPct()));
    }

    @Test
    void boxesPokingOutsideTheImageAreClamped() throws Exception {
        var r = RoboflowService.parseResponse(json("""
            {"image":{"width":100,"height":100},
             "predictions":[{"x":50,"y":50,"width":400,"height":400,"confidence":0.99,"class":"Mixed"}]}
            """), THRESHOLD);

        assertEquals(100.0, r.boundingBoxAreaPct(), 0.05);
    }

    @Test
    void emptyOrBrokenResponsesFallBack() throws Exception {
        assertFalse(RoboflowService.parseResponse(null, THRESHOLD).confident());
        assertFalse(RoboflowService.parseResponse(json("{}"), THRESHOLD).confident());
        assertFalse(RoboflowService.parseResponse(
                json("{\"image\":{\"width\":100,\"height\":100},\"predictions\":[]}"), THRESHOLD).confident());
    }
}
