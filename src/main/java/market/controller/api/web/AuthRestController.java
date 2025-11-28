package market.controller.api.web;

import jakarta.validation.Valid;
import market.domain.Role;
import market.domain.User;
import market.dto.ErrorResponse;
import market.dto.auth.AuthLoginRequestDto;
import market.dto.auth.AuthRegisterRequestDto;
import market.dto.auth.AuthResponseDto;
import market.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;


/**
 * REST-контроллер для операций аутентификации пользователей.
 * <p>Предоставляет HTTP-эндпоинты для входа и регистрации.
 * Все входные данные принимаются в формате JSON, выходные данные
 * также сериализуются в JSON автоматически.</p>
 *
 * <h3>Эндпоинты:</h3>
 * <ul>
 *     <li><b>POST /api/auth/login</b> — авторизация пользователя</li>
 *     <li><b>POST /api/auth/register</b> — регистрация нового пользователя</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final AuthService authService;

    public AuthRestController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Выполняет вход пользователя.
     *
     * @param dto JSON-тело запроса с логином и паролем
     * @return 200 OK — если логин/пароль корректны;<br>
     * 401 Unauthorized — если пользователь не найден или пароль неверный;<br>
     * 400 Bad Request — если JSON некорректный или не прошёл валидацию.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthLoginRequestDto dto) {

        Optional<User> userOpt = authService.login(dto.getUsername(), dto.getPassword());
        if (userOpt.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials"));
        }

        User user = userOpt.get();
        AuthResponseDto responseDto =
                new AuthResponseDto(user.getId(), user.getUsername(), user.getRole());

        return ResponseEntity.ok(responseDto);
    }

    /**
     * Регистрирует нового пользователя.
     *
     * @param dto JSON-тело запроса с логином и паролем
     * @return 201 Created — если регистрация выполнена успешно;<br>
     * 409 Conflict — если пользователь с таким именем уже существует;<br>
     * 400 Bad Request — если JSON некорректный или не прошёл валидацию.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AuthRegisterRequestDto dto) {

        try {
            authService.register(dto.getUsername(), dto.getPassword(), Role.USER);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(ex.getMessage()));
        }

        Optional<User> userOpt = authService.current();

        if (userOpt.isEmpty()) {
            AuthResponseDto responseDto =
                    new AuthResponseDto(null, dto.getUsername(), Role.USER);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        }

        User user = userOpt.get();
        AuthResponseDto responseDto =
                new AuthResponseDto(user.getId(), user.getUsername(), user.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
}
