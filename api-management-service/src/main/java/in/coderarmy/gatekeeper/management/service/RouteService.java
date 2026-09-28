package in.coderarmy.gatekeeper.management.service;

import in.coderarmy.gatekeeper.management.dto.requestDto.HeaderRuleDto;
import in.coderarmy.gatekeeper.management.dto.requestDto.RouteRequest;
import in.coderarmy.gatekeeper.management.dto.responseDto.RouteResponse;
import in.coderarmy.gatekeeper.management.dto.responseDto.VersionResponse;
import in.coderarmy.gatekeeper.management.entity.ApiRoute;
import in.coderarmy.gatekeeper.management.entity.Organization;
import in.coderarmy.gatekeeper.management.entity.RouteConfigurationVersion;
import in.coderarmy.gatekeeper.management.entity.RouteHeaderRule;
import in.coderarmy.gatekeeper.management.exception.ApiException;
import in.coderarmy.gatekeeper.management.repository.ApiRouteRepository;
import in.coderarmy.gatekeeper.management.repository.OrganizationRepository;
import in.coderarmy.gatekeeper.management.repository.RouteVersionRepository;
import in.coderarmy.gatekeeper.management.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RouteService {

    private static final Set<String> HTTP_METHODS =
            Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");

    private final ApiRouteRepository routes;
    private final RouteVersionRepository versions;
    private final OrganizationRepository organizations;
    private final AuditService audit;

    public RouteService(ApiRouteRepository routes, RouteVersionRepository versions,
                        OrganizationRepository organizations, AuditService audit) {
        this.routes = routes;
        this.versions = versions;
        this.organizations = organizations;
        this.audit = audit;
    }

    // ---------- queries ----------

    @Transactional(readOnly = true)
    public List<RouteResponse> list(CurrentUser user, Long organizationId) {
        List<ApiRoute> rows;
        if (user.isPlatformAdmin()) {
            rows = organizationId == null
                    ? routes.findAllByOrderByUpdatedAtDesc()
                    : routes.findByOrganizationIdOrderByUpdatedAtDesc(organizationId);
        } else {
            rows = routes.findByOrganizationIdOrderByUpdatedAtDesc(user.organizationId());
        }
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RouteResponse get(CurrentUser user, Long organizationId, String routeId) {
        return toResponse(find(resolveOrg(user, organizationId), routeId));
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> history(CurrentUser user, Long organizationId, String routeId) {
        Long orgId = resolveOrg(user, organizationId);
        return versions.findByOrganizationIdAndRouteIdOrderByVersionNumberDesc(orgId, routeId).stream()
                .map(v -> new VersionResponse(v.getVersionNumber(), v.getChangeType(),
                        v.getConfigurationSnapshot(), v.getCreatedAt()))
                .toList();
    }

    /** Consumed by the gateway (GATEWAY_SERVICE role): active routes of active organizations only. */
    @Transactional(readOnly = true)
    public List<RouteResponse> activeForGateway() {
        Set<Long> activeOrgs = organizations.findAll().stream()
                .filter(o -> "ACTIVE".equals(o.getStatus()) && o.getDeletedAt() == null)
                .map(Organization::getId)
                .collect(Collectors.toSet());
        return routes.findByActiveTrue().stream()
                .filter(r -> activeOrgs.contains(r.getOrganizationId()))
                .map(this::toResponse)
                .toList();
    }

    // ---------- commands ----------

    @Transactional
    public RouteResponse create(CurrentUser user, Long organizationId, RouteRequest req) {
        long t0 = System.nanoTime();
        Long orgId = resolveOrg(user, organizationId);
        Organization org = organizations.findById(orgId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organization not found"));
        if (!"ACTIVE".equals(org.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Organization is suspended");
        }
        if (routes.existsByOrganizationIdAndRouteId(orgId, req.routeId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Route id '" + req.routeId() + "' already exists");
        }

        ApiRoute route = new ApiRoute();
        route.setOrganizationId(orgId);
        route.setRouteId(req.routeId());
        route.setActive(false);
        route.setConfigurationVersion(1);
        apply(route, req);
        route = routes.save(route);

        snapshot(route, "CREATED");
        audit.record(user.username(), orgId, "CREATE_ROUTE", "ROUTE", route.getRouteId(), "SUCCESS", null, t0);
        return toResponse(route);
    }

    @Transactional
    public RouteResponse update(CurrentUser user, Long organizationId, String routeId, RouteRequest req) {
        long t0 = System.nanoTime();
        Long orgId = resolveOrg(user, organizationId);
        ApiRoute route = find(orgId, routeId);
        if (!route.getRouteId().equals(req.routeId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Route id cannot be changed");
        }
        apply(route, req);
        route.setConfigurationVersion(route.getConfigurationVersion() + 1);
        route = routes.save(route);

        snapshot(route, "UPDATED");
        audit.record(user.username(), orgId, "UPDATE_ROUTE", "ROUTE", routeId, "SUCCESS",
                "Now at version " + route.getConfigurationVersion(), t0);
        return toResponse(route);
    }

    @Transactional
    public RouteResponse setActive(CurrentUser user, Long organizationId, String routeId, boolean active) {
        long t0 = System.nanoTime();
        Long orgId = resolveOrg(user, organizationId);
        ApiRoute route = find(orgId, routeId);
        if (route.isActive() == active) {
            return toResponse(route);
        }
        route.setActive(active);
        route.setConfigurationVersion(route.getConfigurationVersion() + 1);
        route = routes.save(route);

        String type = active ? "ACTIVATED" : "DEACTIVATED";
        snapshot(route, type);
        audit.record(user.username(), orgId, type + "_ROUTE", "ROUTE", routeId, "SUCCESS", null, t0);
        return toResponse(route);
    }

    @Transactional
    public void delete(CurrentUser user, Long organizationId, String routeId) {
        long t0 = System.nanoTime();
        Long orgId = resolveOrg(user, organizationId);
        ApiRoute route = find(orgId, routeId);
        route.setConfigurationVersion(route.getConfigurationVersion() + 1);
        snapshot(route, "DELETED");
        routes.delete(route);
        audit.record(user.username(), orgId, "DELETE_ROUTE", "ROUTE", routeId, "SUCCESS", null, t0);
    }

    // ---------- helpers ----------

    /** Tenants are always pinned to their own organization; platform admins must say which one. */
    private Long resolveOrg(CurrentUser user, Long requested) {
        if (user.isPlatformAdmin()) {
            if (requested == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "organizationId is required for platform admins");
            }
            return requested;
        }
        if (user.organizationId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is not attached to an organization");
        }
        return user.organizationId();
    }

    private ApiRoute find(Long orgId, String routeId) {
        return routes.findByOrganizationIdAndRouteId(orgId, routeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Route not found"));
    }

    private void apply(ApiRoute route, RouteRequest req) {
        String pattern = req.pathPattern().trim();
        if (!pattern.startsWith("/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Path pattern must start with '/'");
        }
        String base = req.targetBaseUrl().trim();
        try {
            URI uri = URI.create(base);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Target base URL must be a valid http(s) URL");
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        Set<String> methods = new LinkedHashSet<>();
        for (String m : req.allowedMethods()) {
            String up = m == null ? "" : m.trim().toUpperCase();
            if (!HTTP_METHODS.contains(up)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported HTTP method: " + m);
            }
            methods.add(up);
        }

        route.setName(req.name().trim());
        route.setPathPattern(pattern);
        route.setTargetBaseUrl(base);
        route.setTargetPathPrefix(req.targetPathPrefix() == null || req.targetPathPrefix().isBlank()
                ? "/" : req.targetPathPrefix().trim());
        route.setRequestsPerMinute(req.requestsPerMinute());
        route.setResponseTimeoutMs(req.responseTimeoutMs() == null ? 5000 : req.responseTimeoutMs());
        route.setIdempotencyEnabled(Boolean.TRUE.equals(req.idempotencyEnabled()));
        route.getAllowedMethods().clear();
        route.getAllowedMethods().addAll(methods);

        route.getHeaderRules().clear();
        if (req.headerRules() != null) {
            for (HeaderRuleDto dto : req.headerRules()) {
                String action = dto.action() == null ? "SET" : dto.action();
                if (!"REMOVE".equals(action) && (dto.headerValue() == null || dto.headerValue().isBlank())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "Header '" + dto.headerName() + "' needs a value for action " + action);
                }
                RouteHeaderRule rule = new RouteHeaderRule();
                rule.setRoute(route);
                rule.setDirection(dto.direction() == null ? "REQUEST" : dto.direction());
                rule.setAction(action);
                rule.setHeaderName(dto.headerName().trim());
                rule.setHeaderValue("REMOVE".equals(action) ? null : dto.headerValue());
                route.getHeaderRules().add(rule);
            }
        }
    }

    private void snapshot(ApiRoute r, String changeType) {
        RouteConfigurationVersion v = new RouteConfigurationVersion();
        v.setOrganizationId(r.getOrganizationId());
        v.setRouteId(r.getRouteId());
        v.setVersionNumber(r.getConfigurationVersion());
        v.setChangeType(changeType);
        v.setConfigurationSnapshot(toJson(r));
        versions.save(v);
    }

    private RouteResponse toResponse(ApiRoute r) {
        List<HeaderRuleDto> rules = r.getHeaderRules().stream()
                .map(h -> new HeaderRuleDto(h.getDirection(), h.getAction(), h.getHeaderName(), h.getHeaderValue()))
                .toList();
        return new RouteResponse(r.getId(), r.getOrganizationId(), r.getRouteId(), r.getName(), r.getPathPattern(),
                r.getTargetBaseUrl(), r.getTargetPathPrefix(), r.getRequestsPerMinute(), r.getResponseTimeoutMs(),
                r.isIdempotencyEnabled(), r.isActive(), r.getConfigurationVersion(),
                List.copyOf(r.getAllowedMethods()), rules, r.getCreatedAt(), r.getUpdatedAt());
    }

    /** Small hand-rolled serializer keeps the snapshot format stable and dependency-free. */
    private String toJson(ApiRoute r) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"routeId\":").append(q(r.getRouteId()));
        sb.append(",\"name\":").append(q(r.getName()));
        sb.append(",\"pathPattern\":").append(q(r.getPathPattern()));
        sb.append(",\"targetBaseUrl\":").append(q(r.getTargetBaseUrl()));
        sb.append(",\"targetPathPrefix\":").append(q(r.getTargetPathPrefix()));
        sb.append(",\"requestsPerMinute\":").append(r.getRequestsPerMinute());
        sb.append(",\"responseTimeoutMs\":").append(r.getResponseTimeoutMs());
        sb.append(",\"idempotencyEnabled\":").append(r.isIdempotencyEnabled());
        sb.append(",\"active\":").append(r.isActive());
        sb.append(",\"allowedMethods\":[")
                .append(r.getAllowedMethods().stream().map(this::q).collect(Collectors.joining(",")))
                .append("]");
        sb.append(",\"headerRules\":[")
                .append(r.getHeaderRules().stream()
                        .map(h -> "{\"direction\":" + q(h.getDirection()) + ",\"action\":" + q(h.getAction())
                                + ",\"headerName\":" + q(h.getHeaderName()) + ",\"headerValue\":" + q(h.getHeaderValue()) + "}")
                        .collect(Collectors.joining(",")))
                .append("]}");
        return sb.toString();
    }

    private String q(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
