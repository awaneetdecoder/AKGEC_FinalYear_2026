package com.swachhdrishti.backend.service;

import org.springframework.stereotype.Service;

/**
 * Converts "what % of the photo is garbage" into a 1-5 severity level and
 * the SLA (hours a worker has to clean it). Thresholds follow the IEEE paper:
 *
 * <pre>
 *  Level 1  coverage  &lt; 5 %      SLA 48 h
 *  Level 2  5  - 15 %          SLA 24 h
 *  Level 3  15 - 35 %          SLA 12 h
 *  Level 4  35 - 60 %          SLA  6 h
 *  Level 5  &gt; 60 %            SLA  0 h  (immediate dispatch)
 * </pre>
 */
@Service
public class SeverityService {

    /** Severity used when the AI is unavailable or not confident. */
    public static final int FALLBACK_SEVERITY = 3;

    /** Immediate-dispatch tickets are shown as breached after this grace period. */
    private static final int IMMEDIATE_GRACE_HOURS = 1;

    public int calculateSeverity(double boundingBoxAreaPct) {
        if (boundingBoxAreaPct < 5) return 1;
        if (boundingBoxAreaPct < 15) return 2;
        if (boundingBoxAreaPct < 35) return 3;
        if (boundingBoxAreaPct < 60) return 4;
        return 5;
    }

    /** Higher severity = shorter deadline. 0 means "immediate dispatch". */
    public int calculateSlaHours(int severity) {
        return switch (severity) {
            case 5 -> 0;
            case 4 -> 6;
            case 3 -> 12;
            case 2 -> 24;
            default -> 48;
        };
    }

    /** The SLA used to compute a due-time: immediate dispatch (0 h) gets a 1 h grace window. */
    public static int effectiveSlaHours(int slaDeadlineHours) {
        return slaDeadlineHours <= 0 ? IMMEDIATE_GRACE_HOURS : slaDeadlineHours;
    }
}
