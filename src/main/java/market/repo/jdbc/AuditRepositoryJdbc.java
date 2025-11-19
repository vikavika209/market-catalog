package market.repo.jdbc;

import market.domain.AuditAction;
import market.domain.AuditEvent;
import market.exception.PersistenceException;
import market.repo.AuditRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-реализация репозитория аудита.
 * <p>
 * Хранит данные в PostgreSQL в таблице {@code market.audit_log}.
 */
public class AuditRepositoryJdbc implements AuditRepository {

    private final DataSource ds;

    public AuditRepositoryJdbc(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public void save(AuditEvent event) {
        String sql = """
                
                    INSERT INTO market.audit_log (username, action, details, ts)
                VALUES (?, ?, ?, ?)
                """;

        LocalDateTime ts = event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now();

        try (Connection cn = ds.getConnection(); PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, event.getUsername());
            ps.setString(2, event.getAction().name());
            ps.setString(3, event.getDetails());
            ps.setTimestamp(4, Timestamp.valueOf(ts));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    event.setId(rs.getLong(1));
                }
            }

            event.setTimestamp(ts);

        } catch (SQLException e) {
            throw wrap("Не удалось сохранить событие аудита", e);
        }
    }

    @Override
    public List<AuditEvent> findAll() {
        String sql = """
                SELECT id, username, action, details, ts
                FROM
                    market.audit_log
                                ORDER BY ts DESC, id DESC
                """;

        try (Connection cn = ds.getConnection(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            List<AuditEvent> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw wrap("Не удалось прочитать журнал аудита", e);
        }
    }

    @Override
    public List<AuditEvent> findByUsername(String username) {
        String sql = """
                SELECT id,
                    username, action,
                    details, ts
                FROM
                    market.audit_log
                WHERE username = ?
                ORDER BY ts DESC, id DESC
                """;

        try (Connection cn = ds.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                List<AuditEvent> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw wrap("Не удалось прочитать аудиты для пользователя username=" + username, e);
        }
    }

    @Override
    public List<AuditEvent> findRecent(int limit) {
        String sql = """
                
                    SELECT id,
                    username, action, details, ts
                FROM market.audit_log
                ORDER BY ts DESC, id DESC
                LIMIT ?
                """;

        try (Connection cn = ds.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                List<AuditEvent> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw wrap("Не удалось прочитать последние " + limit + " событий аудита", e);
        }
    }

    private AuditEvent map(ResultSet rs) throws SQLException {
        AuditEvent e = new AuditEvent();
        e.setId(rs.getLong("id"));
        e.setUsername(rs.getString("username"));
        e.setAction(AuditAction.valueOf(rs.getString("action")));
        e.setDetails(rs.getString("details"));

        Timestamp ts = rs.getTimestamp("ts");
        e.setTimestamp(ts != null ? ts.toLocalDateTime() : null);

        return e;
    }

    private PersistenceException wrap(String action, SQLException e) {
        return new PersistenceException(action + ". Причина: " + e.getMessage());
    }
}
