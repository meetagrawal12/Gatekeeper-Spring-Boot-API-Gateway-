package in.coderarmy.gatekeeper.management.controller;

import in.coderarmy.gatekeeper.management.dto.responseDto.AuditResponse;
import in.coderarmy.gatekeeper.management.dto.responseDto.RouteResponse;
import in.coderarmy.gatekeeper.management.dto.responseDto.StatsResponse;
import in.coderarmy.gatekeeper.management.security.CurrentUser;
import in.coderarmy.gatekeeper.management.service.AdminService;
import in.coderarmy.gatekeeper.management.service.AuditService;
import in.coderarmy.gatekeeper.management.service.RouteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only endpoints: dashboard stats, audit trail, and the gateway's configuration feed. */
@RestController
public class InsightsController {

    private final AdminService adminService;
    private final AuditService auditService;
    private final RouteService routeService;

    public InsightsController(AdminService adminService, AuditService auditService, RouteService routeService) {
        this.adminService = adminService;
        this.auditService = auditService;
        this.routeService = routeService;
    }

    @GetMapping("/stats")
    public StatsResponse stats(@AuthenticationPrincipal Jwt jwt) {
        return adminService.stats(CurrentUser.from(jwt));
    }

    @GetMapping("/audit")
    public List<AuditResponse> audit(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(required = false) Long organizationId) {
        return auditService.list(CurrentUser.from(jwt), organizationId);
    }

    /** GATEWAY_SERVICE only: the live, active route table the gateway should enforce. */
    @GetMapping("/internal/routes")
    public List<RouteResponse> gatewayRoutes() {
        return routeService.activeForGateway();
    }
}
