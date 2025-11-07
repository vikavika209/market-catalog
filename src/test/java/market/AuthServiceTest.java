
package market;
import market.service.AuthService;
import market.domain.Role;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class AuthServiceTest {
    @Test
    void loginSuccessAndFail(){
        AuthService auth = new AuthService();
        assertTrue(auth.login("admin","admin").isPresent());
        assertTrue(auth.current().isPresent());
        auth.logout();
        assertTrue(auth.current().isEmpty());
        assertTrue(auth.login("user","user").isPresent());
        assertTrue(auth.login("user","wrong").isEmpty());
    }
    @Test
    void registerNewUser(){
        AuthService auth = new AuthService();
        String u = "vika";
        auth.register(u,"pass", Role.USER);
        assertTrue(auth.login(u,"pass").isPresent());
    }
}
