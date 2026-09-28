package in.coderarmy.gatekeeper.management.dto.responseDto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String role,
        Long organizationId,
        String organizationName,
        boolean enabled,
        LocalDateTime createdAt) {
}
