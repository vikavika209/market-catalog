package market.service;

import market.domain.AuditEvent;
import market.repo.AuditRepository;

/**
 * Реализация сервиса аудита, отвечающая за запись событий {@link AuditEvent}.
 * Используется для логирования действий пользователей (LOGIN, LOGOUT, CREATE, UPDATE, DELETE, SEARCH).
 */
public class AuditServiceImpl implements AuditService {

    private final AuditRepository auditRepository;

    public AuditServiceImpl(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public void append(AuditEvent e) {
        auditRepository.save(e);
    }
}
