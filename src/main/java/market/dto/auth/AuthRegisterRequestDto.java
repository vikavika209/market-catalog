package market.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO запроса регистрации.
 * Принимается в JSON-формате в /api/auth/register.
 */
public class AuthRegisterRequestDto {

    @NotBlank(message = "username не может быть пустым")
    @Size(min = 3, max = 50, message = "username должен быть от 3 до 50 символов")
    private String username;

    @NotBlank(message = "password не может быть пустым")
    @Size(min = 4, max = 100, message = "password должен быть от 4 до 100 символов")
    private String password;

    public AuthRegisterRequestDto() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
