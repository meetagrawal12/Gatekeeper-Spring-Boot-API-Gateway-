package in.coderarmy.gatekeeper.management.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/** Caller identity, read from the verified JWT claims. */
public record CurrentUser(Long userId, String username, String role, Long organizationId) {

    public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    public static final String TENANT_ADMIN = "TENANT_ADMIN";
    public static final String GATEWAY_SERVICE = "GATEWAY_SERVICE";

    public static CurrentUser from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        Number uid = jwt.getClaim("uid");
        Number org = jwt.getClaim("org");
        return new CurrentUser(
                uid == null ? null : uid.longValue(),
                jwt.getSubject(),
                roles == null || roles.isEmpty() ? null : roles.get(0),
                org == null ? null : org.longValue());
    }

    public boolean isPlatformAdmin() {
        return PLATFORM_ADMIN.equals(role);
    }
}
