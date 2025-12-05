package market.aop;

import com.pet.auditspringbootstarter.audit.CurrentUserProvider;
import market.domain.User;
import market.service.AuthService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthCurrentUserProvider implements CurrentUserProvider {
    private final AuthService authService;

    public AuthCurrentUserProvider(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Optional<String> getCurrentUsername() {
        return authService.current()
                .map(User::getUsername);
    }
}
