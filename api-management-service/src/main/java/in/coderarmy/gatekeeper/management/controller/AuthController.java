package in.coderarmy.gatekeeper.management.controller;

import in.coderarmy.gatekeeper.management.dto.requestDto.LoginRequest;
import in.coderarmy.gatekeeper.management.dto.requestDto.RegisterRequest;
import in.coderarmy.gatekeeper.management.dto.responseDto.AuthResponse;
import in.coderarmy.gatekeeper.management.dto.responseDto.UserResponse;
import in.coderarmy.gatekeeper.management.security.CurrentUser;
import in.coderarmy.gatekeeper.management.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(CurrentUser.from(jwt));
    }
}
