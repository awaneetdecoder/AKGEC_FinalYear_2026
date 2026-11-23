package com.swachhdrishti.backend.controller;

import com.swachhdrishti.backend.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Serves the photos citizens uploaded: GET /uploads/{uuid}.jpg|png|webp
 *
 * Implemented as a plain controller (not a static-resource mapping) so it works
 * even when the project folder has spaces in its path, e.g. "C:\PROJECT 3.0\...".
 * Only files with the exact random names produced by the upload endpoint are served.
 */
@RestController
public class PhotoController {

    private static final Map<String, String> TYPES = Map.of(
            "jpg", "image/jpeg", "png", "image/png", "webp", "image/webp");

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @GetMapping("/uploads/{name:.+}")
    public ResponseEntity<byte[]> photo(@PathVariable String name) throws IOException {
        if (!name.matches("[A-Za-z0-9-]{36}\\.(jpg|png|webp)")) {
            throw new ApiException(404, "Not found.");
        }
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path file = dir.resolve(name).normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) {
            throw new ApiException(404, "Not found.");
        }
        String type = TYPES.get(name.substring(name.lastIndexOf('.') + 1));
        return ResponseEntity.ok()
                .header("Content-Type", type)
                .header("Cache-Control", "public, max-age=86400")
                .body(Files.readAllBytes(file));
    }
}
