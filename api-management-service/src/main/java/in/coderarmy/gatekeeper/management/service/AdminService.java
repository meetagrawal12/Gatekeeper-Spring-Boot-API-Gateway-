package in.coderarmy.gatekeeper.management.service;

import in.coderarmy.gatekeeper.management.dto.responseDto.OrganizationResponse;
import in.coderarmy.gatekeeper.management.dto.responseDto.StatsResponse;
import in.coderarmy.gatekeeper.management.dto.responseDto.UserResponse;
import in.coderarmy.gatekeeper.management.entity.ManagementUser;
import in.coderarmy.gatekeeper.management.entity.Organization;
import in.coderarmy.gatekeeper.management.exception.ApiException;
import in.coderarmy.gatekeeper.management.repository.ApiRouteRepository;
import in.coderarmy.gatekeeper.management.repository.ManagementUserRepository;
import in.coderarmy.gatekeeper.management.repository.OrganizationRepository;
import in.coderarmy.gatekeeper.management.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final OrganizationRepository organizations;
    private final ManagementUserRepository users;
    private final ApiRouteRepository routes;
    private final AuditService audit;
    private final AuthService authService;

    public AdminService(OrganizationRepository organizations, ManagementUserRepository users,
                        ApiRouteRepository routes, AuditService audit, AuthService authService) {
        this.organizations = organizations;
        this.users = users;
        this.routes = routes;
        this.audit = audit;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> listOrganizations() {
        return organizations.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrganizationResponse setOrganizationStatus(CurrentUser actor, Long id, String status) {
        long t0 = System.nanoTime();
        Organization org = organizations.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organization not found"));
        org.setStatus(status);
        org = organizations.save(org);
        audit.record(actor.username(), id, "SET_ORG_STATUS", "ORGANIZATION", String.valueOf(id), "SUCCESS",
                "Status -> " + status, t0);
        return toResponse(org);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        Map<Long, String> orgNames = organizations.findAll().stream()
                .collect(Collectors.toMap(Organization::getId, Organization::getName, (a, b) -> a));
        return users.findAllByOrderByCreatedAtDesc().stream()
                .map(u -> authService.toResponse(u, u.getOrganizationId() == null ? null : orgNames.get(u.getOrganizationId())))
                .toList();
    }

    @Transactional
    public UserResponse setUserEnabled(CurrentUser actor, Long id, boolean enabled) {
        long t0 = System.nanoTime();
        ManagementUser user = users.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getId().equals(actor.userId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot disable your own account");
        }
        user.setEnabled(enabled);
        user = users.save(user);
        audit.record(actor.username(), user.getOrganizationId(), enabled ? "ENABLE_USER" : "DISABLE_USER",
                "USER", user.getUsername(), "SUCCESS", null, t0);
        String orgName = user.getOrganizationId() == null ? null
                : organizations.findById(user.getOrganizationId()).map(Organization::getName).orElse(null);
        return authService.toResponse(user, orgName);
    }

    @Transactional(readOnly = true)
    public StatsResponse stats(CurrentUser user) {
        if (user.isPlatformAdmin()) {
            return new StatsResponse(organizations.count(), users.count(), routes.count(), routes.countByActiveTrue());
        }
        return new StatsResponse(null, null, routes.countByOrganizationId(user.organizationId()),
                routes.countByOrganizationIdAndActiveTrue(user.organizationId()));
    }

    private OrganizationResponse toResponse(Organization o) {
        return new OrganizationResponse(o.getId(), o.getName(), o.getSlug(), o.getStatus(), o.getCreatedAt());
    }
}
