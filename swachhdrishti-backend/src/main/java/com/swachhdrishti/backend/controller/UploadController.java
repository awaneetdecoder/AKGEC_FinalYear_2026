package com.swachhdrishti.backend.controller;

import com.swachhdrishti.backend.exception.ApiException;
import com.swachhdrishti.backend.security.RequestUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * Stores a citizen's garbage photo on the server's disk and returns a URL the
 * complaint can reference. This keeps the Flutter app working with zero cloud
 * setup. In production you can instead upload to Firebase Cloud Storage (as in
 * the paper) and send that URL straight to POST /api/complaints - the rest of
 * the pipeline only cares about the URL.
 *
 * Note: for the REAL Roboflow mode the URL must be reachable from the internet,
 * so deploy the server publicly (or use a tunnel) or use cloud storage.
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private static final Map<String, String> ALLOWED = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping
    public Map<String, String> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request)
            throws IOException {
        RequestUser.from(request).requireRole("CITIZEN");

        String extension = file.getContentType() == null ? null : ALLOWED.get(file.getContentType().toLowerCase());
        if (file.isEmpty() || extension == null) {
            throw new ApiException(400, "Please upload a JPEG, PNG or WebP image.");
        }

        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String name = UUID.randomUUID() + extension;          // random name: never user-controlled
        Files.copy(file.getInputStream(), dir.resolve(name));

        String base = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        return Map.of("url", base + "/uploads/" + name);
    }
}
