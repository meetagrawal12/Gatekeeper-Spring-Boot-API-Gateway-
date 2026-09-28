package in.coderarmy.gatekeeper.management.repository;

import in.coderarmy.gatekeeper.management.entity.ControlPlaneAuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRepository extends JpaRepository<ControlPlaneAuditRecord, Long> {
    List<ControlPlaneAuditRecord> findTop200ByOrderByCreatedAtDesc();
    List<ControlPlaneAuditRecord> findTop200ByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
}
