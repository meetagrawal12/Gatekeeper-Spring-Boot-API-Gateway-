package in.coderarmy.gatekeeper.management.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Self-service onboarding: creates an organization and its TENANT_ADMIN account. */
public record RegisterRequest(
        @NotBlank(message = "Organization name is required")
        @Size(min = 2, max = 150, message = "Organization name must be 2-150 characters")
        String organizationName,

        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,100}$",
                message = "Username must be 3-100 characters: letters, digits, dot, dash, underscore")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain a letter and a digit")
        String password) {
}
