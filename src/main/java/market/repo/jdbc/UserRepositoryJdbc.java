package market.repo.jdbc;

import market.domain.Role;
import market.domain.User;
import market.exception.PersistenceException;
import market.repo.UserRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * JDBC-репозиторий для работы с пользователями системы.
 * <p>
 * Реализует интерфейс {@link UserRepository} и обеспечивает полный CRUD-набор операций,
 * необходимых для аутентификации и управления данными пользователя.
 */
public class UserRepositoryJdbc implements UserRepository {

    private final DataSource ds;

    public UserRepositoryJdbc(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = """
                SELECT id, username, password, role
                FROM market.users
                WHERE username = ?
                """;
        try (Connection cn = ds.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw wrap("Не удалось выполнить поиск пользователя по username=" + username, e);
        }
    }

    @Override
    public void saveUser(User user) {
        if (user.getId() == null) {
            insert(user);
        } else {
            update(user);
        }
    }

    @Override
    public boolean exists(String username) {
        String sql = "SELECT 1 FROM market.users WHERE username = ?";
        try (Connection cn = ds.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw wrap("Не удалось проверить существование пользователя username=" + username, e);
        }
    }

    /**
     * Для JDBC-репозитория загрузка данных из внешнего источника не требуется,
     * поэтому метод ничего не делает.
     */
    @Override
    public void load() {
        // not used: данные читаются напрямую из PostgreSQL по запросам
    }

    /**
     * Для JDBC-репозитория отдельный flush не требуется —
     * все изменения сразу пишутся в БД.
     */
    @Override
    public void persist() {
        // not used: изменения сразу записываются в БД
    }

    // ---------- Внутренние методы ----------

    private void insert(User user) {
        String sql = """
                INSERT INTO market.users (username, password, role)
                VALUES (?, ?, ?)
                RETURNING id
                """;
        try (Connection cn = ds.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole().name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw wrap("Не удалось создать пользователя username=" + user.getUsername(), e);
        }
    }

    private void update(User user) {
        String sql = """
                UPDATE market.users
                   SET username = ?, password = ?, role = ?
                 WHERE id = ?
                """;
        try (Connection cn = ds.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole().name());
            ps.setLong(4, user.getId());

            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new PersistenceException("Не найден пользователь для обновления: id=" + user.getId());
            }
        } catch (SQLException e) {
            throw wrap("Не удалось обновить пользователя id=" + user.getId(), e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRole(Role.valueOf(rs.getString("role")));
        return u;
    }

    /**
     * Оборачивает SQLException в доменное PersistenceException
     * с более читаемым сообщением.
     */
    private PersistenceException wrap(String action, SQLException e) {
        return new PersistenceException(action + ". Причина: " + e.getMessage());
    }
}