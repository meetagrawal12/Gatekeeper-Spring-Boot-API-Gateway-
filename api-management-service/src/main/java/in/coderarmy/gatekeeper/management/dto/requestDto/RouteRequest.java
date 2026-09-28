package in.coderarmy.gatekeeper.management.dto.requestDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record RouteRequest(
        @NotBlank(message = "Route id is required")
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,99}$",
                message = "Route id must be 2-100 chars: lowercase letters, digits and dashes")
        String routeId,

        @NotBlank(message = "Name is required")
        @Size(max = 200)
        String name,

        @NotBlank(message = "Path pattern is required")
        @Size(max = 500)
        String pathPattern,

        @NotBlank(message = "Target base URL is required")
        @Size(max = 1000)
        String targetBaseUrl,

        @Size(max = 500)
        String targetPathPrefix,

        @Min(value = 1, message = "Requests per minute must be at least 1")
        @Max(value = 1_000_000, message = "Requests per minute is too large")
        Integer requestsPerMinute,

        @Min(value = 100, message = "Timeout must be at least 100 ms")
        @Max(value = 120_000, message = "Timeout must be at most 120000 ms")
        Integer responseTimeoutMs,

        Boolean idempotencyEnabled,

        @NotEmpty(message = "Select at least one HTTP method")
        List<String> allowedMethods,

        @Valid
        List<HeaderRuleDto> headerRules) {
}
