package market.controller.api.impl.console;

import market.controller.api.AuthController;
import market.domain.Role;
import market.domain.User;
import market.service.AuthService;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Реализация {@link AuthController}, взаимодействующая с пользователем
 * через консольный интерфейс.
 * <p>
 * Данный контроллер выступает адаптером между UI-слоем (консоль)
 * и сервисным уровнем {@link AuthService}. Он не содержит бизнес-логики,
 * а только делегирует вызовы сервису.
 * <p>
 * Позволяет легко заменить консольный UI на REST API без изменения бизнес-логики.
 */
@Component
public class ConsoleAuthController implements AuthController {
    private final AuthService auth;

    public ConsoleAuthController(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public Optional<User> login(String u, String p) {
        return auth.login(u, p);
    }

    @Override
    public void logout() {
        auth.logout();
    }

    @Override
    public void register(String u, String p, Role r) {
        auth.register(u, p, r);
        auth.current();
    }

    @Override
    public Optional<User> current() {
        return auth.current();
    }

    @Override
    public boolean exists(String username) {
        return auth.exists(username);
    }
}
