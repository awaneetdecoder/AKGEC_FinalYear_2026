package com.swachhdrishti.backend.dto;

import com.swachhdrishti.backend.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Login / first-time registration.
 * Citizens only need e-mail + name. Workers and officers must also send the
 * staff access code configured on the server (staff.access-code).
 * NOTE: a simplified stand-in for Firebase Auth - see AuthService.
 */
@Getter
@Setter
public class LoginRequest {
    @NotBlank
    @Email
    private String email;

    // Required only the first time an account is created.
    private String name;

    @NotNull
    private User.Role role;

    // Required for WORKER and OFFICER.
    private String accessCode;
}
