package com.swachhdrishti.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** What a citizen's app sends when submitting a garbage report. */
@Getter
@Setter
public class ComplaintSubmitRequest {

    // URL of the already-uploaded photo (e.g. a Firebase Storage link).
    @NotBlank
    @Size(max = 500)
    @Pattern(regexp = "^https?://\\S+$", message = "must be an http(s) URL")
    private String photoUrl;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;
}
