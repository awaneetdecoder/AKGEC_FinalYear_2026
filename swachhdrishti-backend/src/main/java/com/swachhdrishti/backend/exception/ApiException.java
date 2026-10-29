package com.swachhdrishti.backend.exception;

// A simple, deliberate error you throw yourself in service/controller code
// (e.g. "Complaint not found") — as opposed to an unexpected crash.
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
