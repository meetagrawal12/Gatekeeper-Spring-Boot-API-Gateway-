package in.coderarmy.gatekeeper.management.controller;

import in.coderarmy.gatekeeper.management.dto.requestDto.RouteRequest;
import in.coderarmy.gatekeeper.management.dto.responseDto.RouteResponse;
import in.coderarmy.gatekeeper.management.dto.responseDto.VersionResponse;
import in.coderarmy.gatekeeper.management.security.CurrentUser;
import in.coderarmy.gatekeeper.management.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Tenant admins manage their own routes; platform admins pass ?organizationId=. */
@RestController
@RequestMapping("/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public List<RouteResponse> list(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(required = false) Long organizationId) {
        return routeService.list(CurrentUser.from(jwt), organizationId);
    }

    @GetMapping("/{routeId}")
    public RouteResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String routeId,
                             @RequestParam(required = false) Long organizationId) {
        return routeService.get(CurrentUser.from(jwt), organizationId, routeId);
    }

    @PostMapping
    public ResponseEntity<RouteResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                @RequestParam(required = false) Long organizationId,
                                                @Valid @RequestBody RouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeService.create(CurrentUser.from(jwt), organizationId, request));
    }

    @PutMapping("/{routeId}")
    public RouteResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable String routeId,
                                @RequestParam(required = false) Long organizationId,
                                @Valid @RequestBody RouteRequest request) {
        return routeService.update(CurrentUser.from(jwt), organizationId, routeId, request);
    }

    @PostMapping("/{routeId}/activate")
    public RouteResponse activate(@AuthenticationPrincipal Jwt jwt, @PathVariable String routeId,
                                  @RequestParam(required = false) Long organizationId) {
        return routeService.setActive(CurrentUser.from(jwt), organizationId, routeId, true);
    }

    @PostMapping("/{routeId}/deactivate")
    public RouteResponse deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable String routeId,
                                    @RequestParam(required = false) Long organizationId) {
        return routeService.setActive(CurrentUser.from(jwt), organizationId, routeId, false);
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String routeId,
                                       @RequestParam(required = false) Long organizationId) {
        routeService.delete(CurrentUser.from(jwt), organizationId, routeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{routeId}/versions")
    public List<VersionResponse> versions(@AuthenticationPrincipal Jwt jwt, @PathVariable String routeId,
                                          @RequestParam(required = false) Long organizationId) {
        return routeService.history(CurrentUser.from(jwt), organizationId, routeId);
    }
}
