package market.repo;

import market.domain.AuditEvent;

import java.util.List;

/**
 * Репозиторий для работы с журналом аудита.
 * <p>
 * Отвечает за запись и чтение событий из таблицы {@code market.audit_log}.
 */
public interface AuditRepository {

    /**
     * Сохраняет новое событие аудита.
     *
     * @param event событие для сохранения
     */
    void save(AuditEvent event);

    /**
     * Возвращает все события аудита,
     * отсортированные по времени (от новых к старым).
     *
     * @return список событий
     */
    List<AuditEvent> findAll();

    /**
     * Возвращает события аудита для конкретного пользователя.
     *
     * @param username имя пользователя
     * @return список событий пользователя
     */
    List<AuditEvent> findByUsername(String username);

    /**
     * Возвращает последние N событий.
     *
     * @param limit максимальное количество событий
     * @return список последних событий
     */
    List<AuditEvent> findRecent(int limit);
}
