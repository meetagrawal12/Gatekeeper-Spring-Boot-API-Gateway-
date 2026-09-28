package in.coderarmy.gatekeeper.management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "api_routes")
@Getter
@Setter
@NoArgsConstructor
public class ApiRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /** Human-chosen identifier, unique per organization. */
    @Column(name = "route_id", nullable = false, length = 100)
    private String routeId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "path_pattern", nullable = false, length = 500)
    private String pathPattern;

    @Column(name = "target_base_url", nullable = false, length = 1000)
    private String targetBaseUrl;

    @Column(name = "target_path_prefix", nullable = false, length = 500)
    private String targetPathPrefix;

    /** Null means "no rate limit". */
    @Column(name = "requests_per_minute")
    private Integer requestsPerMinute;

    @Column(name = "response_timeout_ms", nullable = false)
    private int responseTimeoutMs;

    @Column(name = "idempotency_enabled", nullable = false)
    private boolean idempotencyEnabled;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "configuration_version", nullable = false)
    private long configurationVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "route_allowed_methods", joinColumns = @JoinColumn(name = "route_pk"))
    @Column(name = "http_method", nullable = false, length = 10)
    private Set<String> allowedMethods = new LinkedHashSet<>();

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RouteHeaderRule> headerRules = new ArrayList<>();

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
