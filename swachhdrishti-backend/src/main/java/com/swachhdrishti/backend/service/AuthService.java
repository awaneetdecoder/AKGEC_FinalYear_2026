package com.swachhdrishti.backend.service;

import com.swachhdrishti.backend.dto.LoginRequest;
import com.swachhdrishti.backend.dto.LoginResponse;
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
 * Self-issued JWT login - the stand-in for Firebase Auth described in the
 * paper. It is intentionally simple (no passwords), so:
 *
 *  - CITIZEN accounts are created on first login with just e-mail + name.
 *  - WORKER / OFFICER accounts additionally require the staff access code
 *    (staff.access-code) on EVERY login. Nobody can self-declare as staff.
 *  - An existing account can only log in with the role it was created with.
 *
 * To upgrade to real Firebase Auth later, replace the body of {@link #login}
 * with FirebaseAuth.verifyIdToken(...); everything downstream stays the same.
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
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        User.Role requested = request.getRole();

        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null && existing.getRole() != requested) {
            throw new ApiException(403, "This account is registered as " + existing.getRole()
                    + ", not " + requested + ".");
        }

        if (requested != User.Role.CITIZEN) {
            checkStaffAccessCode(request.getAccessCode());
        }

        User user = existing;
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setName(request.getName() != null && !request.getName().isBlank()
                    ? request.getName().trim()
                    : email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length()));
            user.setRole(requested);
            user = userRepository.save(user);
        }

        String token = jwtService.generateToken(user.getUserId(), user.getEmail(), user.getRole().name());
        return new LoginResponse(token, user.getUserId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    private void checkStaffAccessCode(String provided) {
        if (staffAccessCode == null || staffAccessCode.isBlank()) {
            throw new ApiException(403, "Staff login is disabled on this server (no staff.access-code configured).");
        }
        byte[] expected = staffAccessCode.getBytes(StandardCharsets.UTF_8);
        byte[] actual = provided == null ? new byte[0] : provided.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ApiException(403, "Invalid staff access code.");
        }
    }
}
