package in.coderarmy.gatekeeper.management.repository;

import in.coderarmy.gatekeeper.management.entity.RouteConfigurationVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteVersionRepository extends JpaRepository<RouteConfigurationVersion, Long> {
    List<RouteConfigurationVersion> findByOrganizationIdAndRouteIdOrderByVersionNumberDesc(Long organizationId, String routeId);
}
