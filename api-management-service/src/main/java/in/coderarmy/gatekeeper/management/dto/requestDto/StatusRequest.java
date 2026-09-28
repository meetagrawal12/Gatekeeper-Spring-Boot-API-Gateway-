package in.coderarmy.gatekeeper.management.dto.requestDto;

import jakarta.validation.constraints.Pattern;

public record StatusRequest(
        @Pattern(regexp = "ACTIVE|SUSPENDED", message = "status must be ACTIVE or SUSPENDED")
        String status) {
}
