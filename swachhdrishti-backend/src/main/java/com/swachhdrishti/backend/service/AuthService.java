package com.swachhdrishti.backend.service;

import com.swachhdrishti.backend.dto.LoginRequest;
import com.swachhdrishti.backend.dto.LoginResponse;
import com.swachhdrishti.backend.dto.RegisterRequest;
import com.swachhdrishti.backend.entity.User;
import com.swachhdrishti.backend.exception.ApiException;
import com.swachhdrishti.backend.repository.UserRepository;
import com.swachhdrishti.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Sign-up and sign-in with e-mail + password.
 *
 *  - Anyone can create a CITIZEN account.
 *  - WORKER and OFFICER accounts also need the staff registration code
 *    (staff.access-code, set by the organisation running the server), so a
 *    stranger cannot make themselves an officer. Leave the code empty to
 *    switch staff registration off.
 *  - Passwords are stored only as salted PBKDF2 hashes.
 *  - The role is fixed when the account is created and comes from the account at login.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${staff.access-code:}")
    private String staffAccessCode;

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = normalize(request.getEmail());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ApiException(409, "An account with this e-mail already exists. Please sign in.");
        }
        if (request.getRole() != User.Role.CITIZEN) {
            checkStaffAccessCode(request.getAccessCode());
        }

        User user = new User();
        user.setEmail(email);
        user.setName(request.getName().trim());
        user.setRole(request.getRole());
        user.setPasswordHash(PasswordHasher.hash(request.getPassword()));
        user = userRepository.save(user);

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.getEmail())).orElse(null);

        if (user == null) {
            PasswordHasher.dummyVerify(request.getPassword());
            throw new ApiException(401, "Invalid e-mail or password.");
        }
        if (!PasswordHasher.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(401, "Invalid e-mail or password.");
        }
        return toResponse(user);
    }

    private LoginResponse toResponse(User user) {
        String token = jwtService.generateToken(user.getUserId(), user.getEmail(), user.getRole().name());
        return new LoginResponse(token, user.getUserId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void checkStaffAccessCode(String provided) {
        if (staffAccessCode == null || staffAccessCode.isBlank()) {
            throw new ApiException(403, "Staff registration is closed on this server.");
        }
        byte[] expected = staffAccessCode.getBytes(StandardCharsets.UTF_8);
        byte[] actual = provided == null ? new byte[0] : provided.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ApiException(403, "Invalid staff registration code.");
        }
    }
}
