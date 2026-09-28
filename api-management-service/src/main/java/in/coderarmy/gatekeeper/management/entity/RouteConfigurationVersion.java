package in.coderarmy.gatekeeper.management.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Immutable history row: one per change to a route. */
@Entity
@Table(name = "route_configuration_versions")
@Getter
@Setter
@NoArgsConstructor
public class RouteConfigurationVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "route_id", nullable = false, length = 100)
    private String routeId;

    @Column(name = "version_number", nullable = false)
    private long versionNumber;

    /** CREATED, UPDATED, ACTIVATED, DEACTIVATED, DELETED. */
    @Column(name = "change_type", nullable = false, length = 30)
    private String changeType;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "configuration_snapshot", nullable = false)
    private String configurationSnapshot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
