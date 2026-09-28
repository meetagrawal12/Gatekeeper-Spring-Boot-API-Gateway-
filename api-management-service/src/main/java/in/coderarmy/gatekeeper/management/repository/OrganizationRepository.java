package in.coderarmy.gatekeeper.management.repository;

import in.coderarmy.gatekeeper.management.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsBySlug(String slug);
    List<Organization> findAllByOrderByCreatedAtDesc();
}
