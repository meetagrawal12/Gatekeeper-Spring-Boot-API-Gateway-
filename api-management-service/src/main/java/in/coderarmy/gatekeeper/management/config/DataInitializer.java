package in.coderarmy.gatekeeper.management.config;

import in.coderarmy.gatekeeper.management.dto.requestDto.HeaderRuleDto;
import in.coderarmy.gatekeeper.management.dto.requestDto.RouteRequest;
import in.coderarmy.gatekeeper.management.entity.ManagementUser;
import in.coderarmy.gatekeeper.management.entity.Organization;
import in.coderarmy.gatekeeper.management.entity.Role;
import in.coderarmy.gatekeeper.management.repository.ManagementUserRepository;
import in.coderarmy.gatekeeper.management.repository.OrganizationRepository;
import in.coderarmy.gatekeeper.management.repository.RoleRepository;
import in.coderarmy.gatekeeper.management.security.CurrentUser;
import in.coderarmy.gatekeeper.management.service.RouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The V3 migration seeds platform-admin and gateway-service with password hashes whose plaintext is
 * not documented. Setting the bootstrap properties resets those passwords at startup so you always know them.
 * Optional demo data adds a sample tenant + route for testing.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ManagementUserRepository users;
    private final RoleRepository roles;
    private final OrganizationRepository organizations;
    private final PasswordEncoder encoder;
    private final RouteService routeService;

    @Value("${gatekeeper.bootstrap.platform-admin-password:}")
    private String platformAdminPassword;

    @Value("${gatekeeper.bootstrap.gateway-service-password:}")
    private String gatewayServicePassword;

    @Value("${gatekeeper.bootstrap.demo-data:false}")
    private boolean demoData;

    public DataInitializer(ManagementUserRepository users, RoleRepository roles,
                           OrganizationRepository organizations, PasswordEncoder encoder,
                           RouteService routeService) {
        this.users = users;
        this.roles = roles;
        this.organizations = organizations;
        this.encoder = encoder;
        this.routeService = routeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        resetPassword("platform-admin", platformAdminPassword);
        resetPassword("gateway-service", gatewayServicePassword);
        if (demoData) {
            seedDemo();
        }
    }

    private void resetPassword(String username, String password) {
        if (password == null || password.isBlank()) {
            return;
        }
        users.findByUsername(username).ifPresent(u -> {
            u.setPasswordHash(encoder.encode(password));
            users.save(u);
            log.info("Password for '{}' set from bootstrap configuration", username);
        });
    }

    private void seedDemo() {
        if (users.existsByUsername("demo-tenant")) {
            return;
        }
        Role role = roles.findByName(CurrentUser.TENANT_ADMIN).orElse(null);
        if (role == null) {
            return;
        }
        Organization org = organizations.save(Organization.builder()
                .name("Acme Corp").slug("acme-corp").status("ACTIVE").build());

        ManagementUser user = new ManagementUser();
        user.setOrganizationId(org.getId());
        user.setRole(role);
        user.setUsername("demo-tenant");
        user.setPasswordHash(encoder.encode("Tenant@12345"));
        user.setEnabled(true);
        user = users.save(user);

        CurrentUser actor = new CurrentUser(user.getId(), user.getUsername(), CurrentUser.TENANT_ADMIN, org.getId());
        routeService.create(actor, null, new RouteRequest(
                "orders-api", "Orders API", "/orders/**", "https://httpbin.org", "/anything",
                120, 5000, true, List.of("GET", "POST"),
                List.of(new HeaderRuleDto("REQUEST", "SET", "X-Gateway", "gatekeeper"))));
        routeService.setActive(actor, null, "orders-api", true);
        log.info("Demo data created: tenant 'demo-tenant' (Tenant@12345) with route 'orders-api'");
    }
}
