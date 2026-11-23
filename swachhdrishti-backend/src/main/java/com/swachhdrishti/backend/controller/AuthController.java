package com.swachhdrishti.backend.controller;

import com.swachhdrishti.backend.dto.LoginRequest;
import com.swachhdrishti.backend.dto.LoginResponse;
import com.swachhdrishti.backend.dto.RegisterRequest;
import com.swachhdrishti.backend.dto.UserProfileResponse;
import com.swachhdrishti.backend.exception.ApiException;
import com.swachhdrishti.backend.repository.UserRepository;
import com.swachhdrishti.backend.security.RequestUser;
import com.swachhdrishti.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign-up, sign-in and "who am I". See {@link AuthService}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    // Create an account, then the caller is signed in straight away.
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // Profile + Clean Coins balance for the logged-in user (used by the citizen app).
    @GetMapping("/me")
    public UserProfileResponse me(HttpServletRequest request) {
        RequestUser caller = RequestUser.from(request);
        return userRepository.findById(caller.userId())
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new ApiException(404, "User not found"));
    }
}
