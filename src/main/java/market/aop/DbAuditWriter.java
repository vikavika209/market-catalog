package market.aop;

import com.pet.auditspringbootstarter.audit.AuditWriter;
import market.domain.AuditAction;
import market.domain.AuditEvent;
import market.service.AuditService;
import org.springframework.stereotype.Component;

@Component
public class DbAuditWriter implements AuditWriter {
    private final AuditService auditService;

    public DbAuditWriter(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void write(String username, String actionCode, String details) {
        AuditAction action = AuditAction.valueOf(actionCode);

        AuditEvent event = new AuditEvent(username, action, details);
        auditService.append(event);
    }
}
