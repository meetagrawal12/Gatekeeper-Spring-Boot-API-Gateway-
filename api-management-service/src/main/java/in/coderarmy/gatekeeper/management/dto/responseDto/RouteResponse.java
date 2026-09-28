package in.coderarmy.gatekeeper.management.dto.responseDto;

import in.coderarmy.gatekeeper.management.dto.requestDto.HeaderRuleDto;

import java.time.LocalDateTime;
import java.util.List;

public record RouteResponse(
        Long id,
        Long organizationId,
        String routeId,
        String name,
        String pathPattern,
        String targetBaseUrl,
        String targetPathPrefix,
        Integer requestsPerMinute,
        int responseTimeoutMs,
        boolean idempotencyEnabled,
        boolean active,
        long configurationVersion,
        List<String> allowedMethods,
        List<HeaderRuleDto> headerRules,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
