package market.service;

import market.domain.Role;
import market.domain.User;
import market.exception.PersistenceException;
import market.exception.ValidationException;
import market.repo.UserRepository;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class AuthServiceImpl implements AuthService {
    private final UserRepository repo;
    private User current;

    public AuthServiceImpl(UserRepository repo) {
        this.repo = repo;
        try {
            repo.load();
        } catch (IOException e) {
            throw new PersistenceException("Не удалось загрузить users.csv " + e);
        }

        if (!repo.exists("admin")) repo.saveUser(new User("admin","admin", Role.ADMIN));
        if (!repo.exists("user"))  repo.saveUser(new User("user","user", Role.USER));
        persist();
    }

    @Override
    public Optional<User> login(String username, String password) {
        return repo.findByUsername(username)
                .filter(u -> Objects.equals(password, u.getPassword()))
                .map(u -> { current = u; return u; });
    }

    @Override public void logout() { current = null; }

    @Override public Optional<User> current() { return Optional.ofNullable(current); }

    @Override
    public void register(String username, String password, Role role) {
        if (username==null || username.isBlank()) throw new ValidationException("Имя не может быть пустым");
        if (password==null || password.isBlank()) throw new ValidationException("Пароль не может быть пустым");
        if (repo.exists(username)) throw new ValidationException("Имя уже занято");
        repo.saveUser(new User(username, password, role));
        persist();
    }

    @Override public boolean exists(String username) { return repo.exists(username); }

    @Override
    public void persist() {
        try {
            repo.persist();
        } catch (IOException e) {
            throw new PersistenceException("Не удалось сохранить users.csv " + e);
        }
    }
}