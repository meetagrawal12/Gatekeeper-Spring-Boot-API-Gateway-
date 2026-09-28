package in.coderarmy.gatekeeper.management.dto.responseDto;

import java.time.LocalDateTime;

public record OrganizationResponse(Long id, String name, String slug, String status, LocalDateTime createdAt) {
}
