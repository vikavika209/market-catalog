package market.controller.api;

import market.domain.Role;
import market.domain.User;

import java.util.Optional;

/**
 * Контроллер аутентификации и управления пользователями.
 * <p>
 * Определяет абстрактный интерфейс для входа, выхода и регистрации пользователей.
 * Может иметь разные реализации — консольную, REST API и т.д.
 * <p>
 * Контроллер работает с доменной моделью {@link User} и делегирует бизнес-логику сервисам.
 */
public interface AuthController {
    /**
     * Выполняет вход пользователя по имени и паролю.
     *
     * @param username имя пользователя
     * @param password пароль
     * @return {@link Optional} с объектом {@link User}, если аутентификация прошла успешно;
     * {@link Optional#empty()} — если имя пользователя или пароль неверны
     */
    Optional<User> login(String username, String password);

    /**
     * Выполняет выход текущего пользователя из системы.
     * После вызова этого метода {@link #current()} вернёт {@code Optional.empty()}.
     */
    void logout();

    /**
     * Регистрирует нового пользователя в системе.
     * <p>
     * По умолчанию создаётся пользователь с ролью {@link Role#USER},
     * но реализация может разрешать указание роли вручную.
     *
     * @param username имя нового пользователя
     * @param password пароль нового пользователя
     * @param role     роль (например, {@code USER} или {@code ADMIN})
     * @throws IllegalArgumentException если пользователь с таким именем уже существует
     */
    void register(String username, String password, Role role);

    /**
     * Возвращает текущего авторизованного пользователя, если он есть.
     *
     * @return {@link Optional} с объектом {@link User}, если пользователь вошёл в систему,
     * или {@link Optional#empty()} — если пользователь не авторизован
     */
    Optional<User> current();

    /**
     * Проверяет, существует ли пользователь с указанным именем.
     *
     * @param username имя пользователя для проверки
     * @return {@code true}, если пользователь с таким именем существует; {@code false} — иначе
     */
    boolean exists(String username);
}
