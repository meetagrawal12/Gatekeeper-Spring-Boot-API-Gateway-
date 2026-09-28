package in.coderarmy.gatekeeper.management.repository;

import in.coderarmy.gatekeeper.management.entity.ApiRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiRouteRepository extends JpaRepository<ApiRoute, Long> {
    List<ApiRoute> findAllByOrderByUpdatedAtDesc();
    List<ApiRoute> findByOrganizationIdOrderByUpdatedAtDesc(Long organizationId);
    Optional<ApiRoute> findByOrganizationIdAndRouteId(Long organizationId, String routeId);
    boolean existsByOrganizationIdAndRouteId(Long organizationId, String routeId);
    List<ApiRoute> findByActiveTrue();
    long countByOrganizationId(Long organizationId);
    long countByActiveTrue();
    long countByOrganizationIdAndActiveTrue(Long organizationId);
}
