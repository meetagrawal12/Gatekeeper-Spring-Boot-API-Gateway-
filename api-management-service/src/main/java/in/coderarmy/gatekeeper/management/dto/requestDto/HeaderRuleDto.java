package in.coderarmy.gatekeeper.management.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HeaderRuleDto(
        @Pattern(regexp = "REQUEST|RESPONSE", message = "direction must be REQUEST or RESPONSE")
        String direction,

        @Pattern(regexp = "ADD|SET|REMOVE", message = "action must be ADD, SET or REMOVE")
        String action,

        @NotBlank(message = "Header name is required")
        @Size(max = 200)
        String headerName,

        @Size(max = 2000)
        String headerValue) {
}
