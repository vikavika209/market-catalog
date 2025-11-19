package market.service.jdbc;

import market.domain.Role;
import market.domain.User;
import market.exception.PersistenceException;
import market.exception.ValidationException;
import market.repo.UserRepository;
import market.service.AuthService;

import java.io.IOException;
import java.util.Optional;

/**
 * Сервис аутентификации и управления пользователями,
 * работающий поверх JDBC-репозитория.
 * <p>
 * Хранит "текущего пользователя" в памяти процесса (для консольного приложения).
 */
public class UserServiceJdbc implements AuthService {

    private final UserRepository repo;
    private User currentUser;

    public UserServiceJdbc(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<User> login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Имя пользователя не может быть пустым");
        }

        if (password == null || password.isBlank()) {
            throw new ValidationException("Пароль не может быть пустым");
        }

        return repo.findByUsername(username)
                .filter(u -> u.getPassword().equals(password))
                .map(u -> {
                    currentUser = u;
                    return u;
                });
    }

    @Override
    public void logout() {
        currentUser = null;
    }

    @Override
    public Optional<User> current() {
        return Optional.ofNullable(currentUser);
    }

    @Override
    public void register(String username, String password, Role role) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Имя пользователя не может быть пустым");
        }

        if (password == null || password.isBlank()) {
            throw new ValidationException("Пароль не может быть пустым");
        }

        if (role == null) {
            throw new ValidationException("Роль пользователя обязательна");
        }

        if (repo.exists(username)) {
            throw new ValidationException("Пользователь с таким именем уже существует");
        }

        User u = new User();
        u.setUsername(username.trim());
        u.setPassword(password);
        u.setRole(role);

        try {
            repo.saveUser(u);
        } catch (PersistenceException e) {
            throw e;
        }
    }

    @Override
    public boolean exists(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return repo.exists(username);
    }

    @Override
    public void persist() {
        try {
            repo.persist();
        } catch (IOException e) {
            throw new PersistenceException("Ошибка сохранения пользователей: " + e.getMessage());
        }
    }
}
