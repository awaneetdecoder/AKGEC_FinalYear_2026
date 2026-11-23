package com.swachhdrishti.backend.dto;

import com.swachhdrishti.backend.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Create-account form. Workers and officers must also give the staff registration code. */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(max = 80)
    private String name;

    @NotBlank
    @Email
    @Size(max = 120)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "must be 8 to 72 characters")
    private String password;

    @NotNull
    private User.Role role;

    // Required for WORKER and OFFICER only.
    private String accessCode;
}
