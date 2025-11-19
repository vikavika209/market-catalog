package market.service.jdbc;

import market.domain.AuditEvent;
import market.repo.AuditRepository;
import market.service.AuditService;

/**
 * JDBC-репозиторий для сущности {@link AuditEvent}.
 * Все события аудита сохраняются в PostgreSQL (schema: market, table: audit_log).
 */
public class AuditServiceJdbc implements AuditService {

    private final AuditRepository repo;

    public AuditServiceJdbc(AuditRepository repo) {
        this.repo = repo;
    }

    @Override
    public void append(AuditEvent e) {
        repo.save(e);
    }
}