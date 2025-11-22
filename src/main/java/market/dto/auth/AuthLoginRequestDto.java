package market.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO запроса логина.
 * Принимается в JSON-формате в /api/auth/login.
 */
public class AuthLoginRequestDto {

    @NotBlank(message = "username не может быть пустым")
    private String username;

    @NotBlank(message = "password не может быть пустым")
    private String password;

    public AuthLoginRequestDto() {
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
