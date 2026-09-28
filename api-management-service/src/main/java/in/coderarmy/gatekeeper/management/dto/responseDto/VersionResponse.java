package in.coderarmy.gatekeeper.management.dto.responseDto;

import java.time.LocalDateTime;

public record VersionResponse(long versionNumber, String changeType, String configurationSnapshot, LocalDateTime createdAt) {
}
