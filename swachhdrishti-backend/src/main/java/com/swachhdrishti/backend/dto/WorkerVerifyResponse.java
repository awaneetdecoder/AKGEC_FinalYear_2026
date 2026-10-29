package com.swachhdrishti.backend.dto;

public record WorkerVerifyResponse(boolean verified, double distanceMeters, String zoneName, String message) {
}
