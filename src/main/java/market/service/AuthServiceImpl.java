package market.service;

import com.pet.auditspringbootstarter.audit.Audited;
import com.pet.loggingspringbootstarter.logging.Logged;
import market.domain.Role;
import market.domain.User;
import market.exception.PersistenceException;
import market.exception.ValidationException;
import market.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Реализация сервиса аутентификации пользователей.
 * Отвечает за вход, выход, регистрацию и получение текущего пользователя.
 * Работает поверх {@link UserRepository}, который управляет хранением данных.
 */
@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository repo;
    private User current;

    public AuthServiceImpl(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    @Logged
    @Audited("LOGIN")
    public Optional<User> login(String username, String password) {
        return repo.findByUsername(username).filter(u -> Objects.equals(password, u.getPassword())).map(u -> {
            current = u;
            return u;
        });
    }

    @Override
    @Logged
    @Audited("LOGOUT")
    public void logout() {
        current = null;
    }

    @Override
    @Logged
    public Optional<User> current() {
        return Optional.ofNullable(current);
    }

    @Override
    @Logged
    @Audited("CREATE")
    public void register(String username, String password, Role role) {
        if (username == null || username.isBlank()) throw new ValidationException("Имя не может быть пустым");
        if (password == null || password.isBlank()) throw new ValidationException("Пароль не может быть пустым");
        if (repo.exists(username)) throw new ValidationException("Имя уже занято");
        repo.saveUser(new User(username, password, role));
        persist();
    }

    @Override
    @Logged
    public boolean exists(String username) {
        return repo.exists(username);
    }

    @Override
    @Logged
    public void persist() {
        try {
            repo.persist();
        } catch (IOException e) {
            throw new PersistenceException("Не удалось сохранить users.csv " + e);
        }
    }
}