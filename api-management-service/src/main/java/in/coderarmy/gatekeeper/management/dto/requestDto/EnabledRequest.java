package in.coderarmy.gatekeeper.management.dto.requestDto;

import jakarta.validation.constraints.NotNull;

public record EnabledRequest(@NotNull(message = "enabled is required") Boolean enabled) {
}
