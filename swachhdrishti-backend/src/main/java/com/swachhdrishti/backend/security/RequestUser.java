package com.swachhdrishti.backend.security;

import com.swachhdrishti.backend.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;

/** The authenticated caller, as established by {@link JwtAuthFilter}. */
public record RequestUser(Long userId, String email, String role) {

    public static RequestUser from(HttpServletRequest request) {
        Object id = request.getAttribute("userId");
        Object role = request.getAttribute("role");
        if (!(id instanceof Long) || !(role instanceof String)) {
            throw new ApiException(401, "Not authenticated.");
        }
        return new RequestUser((Long) id, (String) request.getAttribute("email"), (String) role);
    }

    /** Throws 403 unless the caller has one of the given roles. Returns this for chaining. */
    public RequestUser requireRole(String... allowed) {
        if (Arrays.stream(allowed).noneMatch(r -> r.equals(role))) {
            throw new ApiException(403, "Your role (" + role + ") is not allowed to do this. Required: "
                    + String.join(" or ", allowed) + ".");
        }
        return this;
    }
}
