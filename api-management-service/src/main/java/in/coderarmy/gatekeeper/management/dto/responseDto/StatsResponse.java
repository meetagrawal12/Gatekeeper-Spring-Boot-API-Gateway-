package in.coderarmy.gatekeeper.management.dto.responseDto;

/** organizations/users are null for tenant-scoped callers. */
public record StatsResponse(Long organizations, Long users, long routes, long activeRoutes) {
}
