package market.controller.api.impl.console;

import market.controller.api.AuthController;
import market.domain.Role;
import market.domain.User;
import market.service.AuthService;

import java.util.Optional;

public class ConsoleAuthController implements AuthController {
    private final AuthService auth;

    public ConsoleAuthController(AuthService auth){
        this.auth = auth;
    }

    @Override
    public Optional<User> login(String u, String p){
        return auth.login(u, p);
    }

    @Override
    public void logout(){
        auth.logout();
    }

    @Override
    public User register(String u, String p, Role r){
        auth.register(u, p, r);
        return auth.current().orElse(null);
    }

    @Override
    public Optional<User> current(){
        return auth.current();
    }

    @Override
    public boolean exists(String username){
        return auth.exists(username);
    }
}
