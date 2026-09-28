package in.coderarmy.gatekeeper.management.service;

import in.coderarmy.gatekeeper.management.dto.responseDto.AuditResponse;
import in.coderarmy.gatekeeper.management.entity.ControlPlaneAuditRecord;
import in.coderarmy.gatekeeper.management.repository.AuditRepository;
import in.coderarmy.gatekeeper.management.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    /** Runs in its own transaction so failures are still recorded when the caller rolls back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String actor, Long organizationId, String action, String resourceType,
                       String resourceId, String result, String details, long startedAtNanos) {
        ControlPlaneAuditRecord r = new ControlPlaneAuditRecord();
        r.setActor(truncate(actor, 100));
        r.setOrganizationId(organizationId);
        r.setAction(truncate(action, 100));
        r.setResourceType(truncate(resourceType, 100));
        r.setResourceId(truncate(resourceId, 100));
        r.setResult(result);
        r.setDetails(truncate(details, 2000));
        r.setDurationMs((System.nanoTime() - startedAtNanos) / 1_000_000);
        repository.save(r);
    }

    @Transactional(readOnly = true)
    public List<AuditResponse> list(CurrentUser user, Long organizationId) {
        List<ControlPlaneAuditRecord> rows;
        if (user.isPlatformAdmin()) {
            rows = organizationId == null
                    ? repository.findTop200ByOrderByCreatedAtDesc()
                    : repository.findTop200ByOrganizationIdOrderByCreatedAtDesc(organizationId);
        } else {
            rows = repository.findTop200ByOrganizationIdOrderByCreatedAtDesc(user.organizationId());
        }
        return rows.stream().map(r -> new AuditResponse(r.getId(), r.getOrganizationId(), r.getActor(),
                r.getAction(), r.getResourceType(), r.getResourceId(), r.getResult(), r.getDurationMs(),
                r.getDetails(), r.getCreatedAt())).toList();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
