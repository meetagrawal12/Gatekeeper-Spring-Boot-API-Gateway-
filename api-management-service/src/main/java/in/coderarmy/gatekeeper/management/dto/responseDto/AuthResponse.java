package in.coderarmy.gatekeeper.management.dto.responseDto;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {
}
