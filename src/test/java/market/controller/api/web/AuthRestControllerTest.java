package market.controller.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import market.domain.Role;
import market.domain.User;
import market.dto.auth.AuthLoginRequestDto;
import market.dto.auth.AuthRegisterRequestDto;
import market.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthRestControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthRestController authController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void login_ok() throws Exception {
        User user = new User("newuser", "123", Role.USER);
        user.setId(1L);

        when(authService.login("newuser", "123"))
                .thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newuser","password":"123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void login_invalidCredentials() throws Exception {
        AuthLoginRequestDto dto = new AuthLoginRequestDto();
        dto.setUsername("user");
        dto.setPassword("wrong");

        when(authService.login(anyString(), anyString()))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void register_success_withCurrentUser() throws Exception {
        AuthRegisterRequestDto dto = new AuthRegisterRequestDto();
        dto.setUsername("newuser");
        dto.setPassword("12345");

        User user = new User();
        user.setId(10L);
        user.setUsername("newuser");
        user.setRole(Role.USER);

        when(authService.current()).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_success_withoutCurrentUser() throws Exception {
        AuthRegisterRequestDto dto = new AuthRegisterRequestDto();
        dto.setUsername("ghost");
        dto.setPassword("pwd");

        when(authService.current()).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ghost"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_conflict_usernameAlreadyExists() throws Exception {
        AuthRegisterRequestDto dto = new AuthRegisterRequestDto();
        dto.setUsername("existing");
        dto.setPassword("pwd");

        doThrow(new IllegalArgumentException("User already exists"))
                .when(authService)
                .register("existing", "pwd", Role.USER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User already exists"));
    }
}