package in.coderarmy.gatekeeper.management.service;

import in.coderarmy.gatekeeper.management.dto.requestDto.LoginRequest;
import in.coderarmy.gatekeeper.management.dto.requestDto.RegisterRequest;
import in.coderarmy.gatekeeper.management.dto.responseDto.AuthResponse;
import in.coderarmy.gatekeeper.management.dto.responseDto.UserResponse;
import in.coderarmy.gatekeeper.management.entity.ManagementUser;
import in.coderarmy.gatekeeper.management.entity.Organization;
import in.coderarmy.gatekeeper.management.entity.Role;
import in.coderarmy.gatekeeper.management.exception.ApiException;
import in.coderarmy.gatekeeper.management.repository.ManagementUserRepository;
import in.coderarmy.gatekeeper.management.repository.OrganizationRepository;
import in.coderarmy.gatekeeper.management.repository.RoleRepository;
import in.coderarmy.gatekeeper.management.security.CurrentUser;
import in.coderarmy.gatekeeper.management.security.JwtService;
import in.coderarmy.gatekeeper.management.security.LoginAttemptService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final ManagementUserRepository users;
    private final RoleRepository roles;
    private final OrganizationRepository organizations;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService audit;
    private final LoginAttemptService attempts;

    public AuthService(ManagementUserRepository users, RoleRepository roles, OrganizationRepository organizations,
                       PasswordEncoder passwordEncoder, JwtService jwtService, AuditService audit,
                       LoginAttemptService attempts) {
        this.users = users;
        this.roles = roles;
        this.organizations = organizations;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.audit = audit;
        this.attempts = attempts;
    }

    public AuthResponse login(LoginRequest request) {
        long t0 = System.nanoTime();
        String username = request.username().trim();

        if (attempts.isLocked(username)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed sign-in attempts. Try again in a few minutes.");
        }

        ManagementUser user = users.findByUsername(username).orElse(null);
        boolean valid = user != null && user.isEnabled()
                && passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (!valid) {
            attempts.failed(username);
            audit.record(username, null, "LOGIN", "SESSION", username, "FAILURE", "Invalid credentials", t0);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        Organization org = null;
        if (user.getOrganizationId() != null) {
            org = organizations.findById(user.getOrganizationId()).orElse(null);
            if (org == null || !"ACTIVE".equals(org.getStatus())) {
                audit.record(username, user.getOrganizationId(), "LOGIN", "SESSION", username, "FAILURE",
                        "Organization not active", t0);
                throw new ApiException(HttpStatus.FORBIDDEN, "Your organization is suspended. Contact support.");
            }
        }

        attempts.succeeded(username);
        audit.record(username, user.getOrganizationId(), "LOGIN", "SESSION", username, "SUCCESS", null, t0);
        return buildResponse(user, org == null ? null : org.getName());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        long t0 = System.nanoTime();
        String orgName = request.organizationName().trim();
        String username = request.username().trim();

        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username is already taken");
        }
        if (organizations.existsByNameIgnoreCase(orgName)) {
            throw new ApiException(HttpStatus.CONFLICT, "An organization with this name already exists");
        }

        Role tenantAdmin = roles.findByName(CurrentUser.TENANT_ADMIN)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Role TENANT_ADMIN is not seeded"));

        Organization org = Organization.builder()
                .name(orgName)
                .slug(uniqueSlug(orgName))
                .status("ACTIVE")
                .build();
        org = organizations.save(org);

        ManagementUser user = new ManagementUser();
        user.setOrganizationId(org.getId());
        user.setRole(tenantAdmin);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user = users.save(user);

        audit.record(username, org.getId(), "REGISTER_TENANT", "ORGANIZATION", String.valueOf(org.getId()),
                "SUCCESS", "Organization " + org.getSlug() + " created", t0);
        return buildResponse(user, org.getName());
    }

    @Transactional(readOnly = true)
    public UserResponse me(CurrentUser current) {
        ManagementUser user = users.findById(current.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Account no longer exists"));
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Account is disabled");
        }
        String orgName = user.getOrganizationId() == null ? null
                : organizations.findById(user.getOrganizationId()).map(Organization::getName).orElse(null);
        return toResponse(user, orgName);
    }

    public UserResponse toResponse(ManagementUser user, String organizationName) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole().getName(),
                user.getOrganizationId(), organizationName, user.isEnabled(), user.getCreatedAt());
    }

    private AuthResponse buildResponse(ManagementUser user, String orgName) {
        return new AuthResponse(jwtService.issue(user), "Bearer", jwtService.ttlSeconds(), toResponse(user, orgName));
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (base.isEmpty()) {
            base = "org";
        }
        if (base.length() > 80) {
            base = base.substring(0, 80);
        }
        String slug = base;
        int i = 2;
        while (organizations.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}
