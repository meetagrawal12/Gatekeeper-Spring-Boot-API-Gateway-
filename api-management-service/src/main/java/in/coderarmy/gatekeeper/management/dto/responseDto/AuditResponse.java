package in.coderarmy.gatekeeper.management.dto.responseDto;

import java.time.LocalDateTime;

public record AuditResponse(
        Long id,
        Long organizationId,
        String actor,
        String action,
        String resourceType,
        String resourceId,
        String result,
        long durationMs,
        String details,
        LocalDateTime createdAt) {
}
