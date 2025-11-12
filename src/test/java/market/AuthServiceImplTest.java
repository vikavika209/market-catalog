
package market;
import market.repo.InMemoryUserRepository;
import market.domain.Role;
import market.repo.UserRepository;
import market.service.AuthService;
import market.service.AuthServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthServiceImplTest {
    @Test
    void loginSuccessAndFail(){
        UserRepository repo = new InMemoryUserRepository();
        AuthService auth = new AuthServiceImpl(repo);
        assertTrue(auth.login("admin","admin").isPresent());
        assertTrue(auth.current().isPresent());
        auth.logout();
        assertTrue(auth.current().isEmpty());
        assertTrue(auth.login("user","user").isPresent());
        assertTrue(auth.login("user","wrong").isEmpty());
    }
    @Test
    void registerNewUser(){
        UserRepository repo =  new InMemoryUserRepository();
        AuthService auth = new AuthServiceImpl(repo);
        String u = "vika";
        auth.register(u,"pass", Role.USER);
        assertTrue(auth.login(u,"pass").isPresent());
    }
}
