package com.swachhdrishti.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// A clean, consistent error shape returned to the frontend instead of a
// raw Java stack trace, whenever something goes wrong.
@Getter
@AllArgsConstructor
public class ApiError {
    private int status;
    private String message;
}
