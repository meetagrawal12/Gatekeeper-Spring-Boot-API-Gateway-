package in.coderarmy.gatekeeper.management.controller;

import in.coderarmy.gatekeeper.management.dto.requestDto.EnabledRequest;
import in.coderarmy.gatekeeper.management.dto.requestDto.StatusRequest;
import in.coderarmy.gatekeeper.management.dto.responseDto.OrganizationResponse;
import in.coderarmy.gatekeeper.management.dto.responseDto.UserResponse;
import in.coderarmy.gatekeeper.management.security.CurrentUser;
import in.coderarmy.gatekeeper.management.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** PLATFORM_ADMIN only (enforced in SecurityConfig). */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/organizations")
    public List<OrganizationResponse> organizations() {
        return adminService.listOrganizations();
    }

    @PatchMapping("/organizations/{id}/status")
    public OrganizationResponse setStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                          @Valid @RequestBody StatusRequest request) {
        return adminService.setOrganizationStatus(CurrentUser.from(jwt), id, request.status());
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return adminService.listUsers();
    }

    @PatchMapping("/users/{id}/enabled")
    public UserResponse setEnabled(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                   @Valid @RequestBody EnabledRequest request) {
        return adminService.setUserEnabled(CurrentUser.from(jwt), id, request.enabled());
    }
}
