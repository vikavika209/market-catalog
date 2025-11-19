package market.service;

import market.domain.Role;
import market.domain.User;

import java.util.Optional;

/**
 * Сервис аутентификации и управления пользователями.
 * <p>
 * Отвечает за бизнес-логику входа, выхода, регистрацию новых пользователей,
 * а также за сохранение и загрузку данных пользователей через репозиторий.
 * <p>
 * Сервис изолирует доменный уровень от деталей хранения данных.
 */
public interface AuthService {
    /**
     * Выполняет вход пользователя по имени и паролю.
     *
     * @param username имя пользователя
     * @param password пароль пользователя
     * @return {@link Optional} с объектом {@link User}, если вход выполнен успешно;
     * {@link Optional#empty()} — если имя пользователя или пароль неверны
     */
    Optional<User> login(String username, String password);

    /**
     * Выполняет выход текущего авторизованного пользователя.
     * После вызова этого метода {@link #current()} вернёт {@code Optional.empty()}.
     */
    void logout();

    /**
     * Возвращает текущего авторизованного пользователя, если он есть.
     *
     * @return {@link Optional} с объектом {@link User}, если пользователь авторизован;
     * {@link Optional#empty()}, если в системе никто не вошёл
     */
    Optional<User> current();

    /**
     * Регистрирует нового пользователя в системе.
     * <p>
     * Проверяет уникальность имени и валидирует вводимые данные.
     * По умолчанию создаётся пользователь с ролью {@link Role#USER}.
     *
     * @param username имя нового пользователя
     * @param password пароль нового пользователя
     * @param role     роль (например, {@link Role#USER} или {@link Role#ADMIN})
     * @throws IllegalArgumentException если пользователь с таким именем уже существует
     */
    void register(String username, String password, Role role);

    /**
     * Проверяет, существует ли пользователь с указанным именем.
     *
     * @param username имя пользователя
     * @return {@code true}, если пользователь с таким именем существует; {@code false} — иначе
     */
    boolean exists(String username);

    /**
     * Сохраняет текущее состояние пользователей в постоянное хранилище.
     * <p>
     * Может использоваться, например, для записи данных в CSV-файл.
     */
    void persist();
}