package market.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import market.domain.Role;
import market.domain.User;
import market.dto.ErrorResponse;
import market.dto.auth.AuthLoginRequestDto;
import market.dto.auth.AuthRegisterRequestDto;
import market.dto.auth.AuthResponseDto;
import market.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServletTest {

    private AuthServlet servlet;

    @Mock
    private AuthService authService;

    @Mock
    private Validator validator;

    @Mock
    private HttpServletRequest req;

    @Mock
    private HttpServletResponse resp;

    @Mock
    private DataSource ds;

    private ObjectMapper objectMapper;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setup() throws Exception {
        servlet = new AuthServlet();

        objectMapper = new ObjectMapper();
        responseBody = new ByteArrayOutputStream();

        inject(servlet, "mapper", objectMapper);
        inject(servlet, "validator", validator);
        inject(servlet, "authService", authService);

        when(resp.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }

            @Override
            public void write(int b) {
                responseBody.write(b);
            }
        });

        when(req.getParameter(anyString())).thenReturn(null);
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        var f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private ServletInputStream json(String json) {
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
        return new ServletInputStream() {
            @Override
            public int read() {
                return bais.read();
            }

            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(jakarta.servlet.ReadListener listener) {

            }
        };
    }

    private <T> T read(Class<T> clazz) throws IOException {
        return objectMapper.readValue(responseBody.toByteArray(), clazz);
    }

    private <T> T read(TypeReference<T> type) throws IOException {
        return objectMapper.readValue(responseBody.toByteArray(), type);
    }

    @Test
    void init_throws_if_no_datasource() {
        ServletConfig config = mock(ServletConfig.class);
        ServletContext ctx = mock(ServletContext.class);

        when(config.getServletContext()).thenReturn(ctx);
        when(ctx.getAttribute("ds")).thenReturn(null);

        assertThrows(Exception.class, () -> {
            AuthServlet servlet = new AuthServlet();
            servlet.init(config);
        });
    }

    @Test
    void init_ok_when_ds_present() throws Exception {
        ServletConfig config = mock(ServletConfig.class);
        ServletContext ctx = mock(ServletContext.class);

        when(config.getServletContext()).thenReturn(ctx);
        when(ctx.getAttribute("ds")).thenReturn(ds);

        AuthServlet servlet = new AuthServlet();

        inject(servlet, "authService", mock(AuthService.class));
        inject(servlet, "validator", mock(Validator.class));
        inject(servlet, "mapper", new ObjectMapper());

        var f = AuthServlet.class.getDeclaredField("authService");
        f.setAccessible(true);
        assertNotNull(f.get(servlet));
    }

    @Test
    void login_invalid_json_returns400() throws Exception {
        when(req.getPathInfo()).thenReturn("/login");
        when(req.getInputStream()).thenReturn(json("bad_json"));

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        ErrorResponse body = read(ErrorResponse.class);
        assertTrue(body.getMessage().contains("Invalid JSON"));
    }

    @Test
    void login_validation_errors_return400() throws Exception {
        when(req.getPathInfo()).thenReturn("/login");
        when(req.getInputStream()).thenReturn(json("""
                {"username":"a", "password":""}
                """));

        @SuppressWarnings("unchecked")
        ConstraintViolation<AuthLoginRequestDto> v = mock(ConstraintViolation.class);
        Path path = mock(Path.class);

        when(path.toString()).thenReturn("password");
        when(v.getPropertyPath()).thenReturn(path);
        when(v.getMessage()).thenReturn("must not be empty");

        when(validator.validate(any(AuthLoginRequestDto.class)))
                .thenReturn(Set.of(v));

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);

        Map<String, Object> body = read(new TypeReference<>() {
        });

        @SuppressWarnings("unchecked")
        List<Map<String, String>> errors = (List<Map<String, String>>) body.get("errors");

        assertEquals(1, errors.size());
        assertEquals("password", errors.getFirst().get("field"));
        assertEquals("must not be empty", errors.getFirst().get("message"));
    }

    @Test
    void login_invalid_credentials_returns401() throws Exception {
        when(req.getPathInfo()).thenReturn("/login");
        when(req.getInputStream()).thenReturn(json("""
                {"username":"vika","password":"wrong"}
                """));

        when(validator.validate(any())).thenReturn(Set.of());
        when(authService.login(anyString(), anyString()))
                .thenReturn(Optional.empty());

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponse body = read(ErrorResponse.class);
        assertTrue(body.getMessage().contains("Invalid credentials"));
    }

    @Test
    void login_ok_returns200_and_user_dto() throws Exception {
        when(req.getPathInfo()).thenReturn("/login");
        when(req.getInputStream()).thenReturn(json("""
                {"username":"vika","password":"123"}
                """));

        when(validator.validate(any())).thenReturn(Set.of());

        User u = new User(100L, "vika", "hash", Role.USER);
        when(authService.login("vika", "123"))
                .thenReturn(Optional.of(u));

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        AuthResponseDto dto = read(AuthResponseDto.class);

        assertEquals(100L, dto.getId());
        assertEquals("vika", dto.getUsername());
        assertEquals(Role.USER, dto.getRole());
    }

    @Test
    void register_invalid_json_returns400() throws Exception {
        when(req.getPathInfo()).thenReturn("/register");
        when(req.getInputStream()).thenReturn(json("bad json"));

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);

        ErrorResponse body = read(ErrorResponse.class);
        assertTrue(body.getMessage().contains("Invalid JSON"));
    }

    @Test
    void register_validation_errors_return400() throws Exception {
        when(req.getPathInfo()).thenReturn("/register");
        when(req.getInputStream()).thenReturn(json("""
                {"username":"","password":"123"}
                """));

        @SuppressWarnings("unchecked")
        ConstraintViolation<AuthRegisterRequestDto> v = mock(ConstraintViolation.class);
        Path path = mock(Path.class);

        when(path.toString()).thenReturn("username");
        when(v.getPropertyPath()).thenReturn(path);
        when(v.getMessage()).thenReturn("must not be empty");

        Set<ConstraintViolation<AuthRegisterRequestDto>> violations = Set.of(v);
        when(validator.validate(any(AuthRegisterRequestDto.class))).thenReturn(violations);

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);

        Map<String, Object> body = read(new TypeReference<>() {
        });

        @SuppressWarnings("unchecked")
        List<Map<String, String>> errors = (List<Map<String, String>>) body.get("errors");

        assertEquals(1, errors.size());
        assertEquals("username",
                errors.getFirst().get("field"));

        assertEquals("must not be empty", errors.getFirst().get("message"));
    }

    @Test
    void register_username_exists_returns409() throws Exception {
        when(req.getPathInfo()).thenReturn("/register");
        when(req.getInputStream()).thenReturn(json("""
                {"username":"vika", "password":"123"}
                """));

        when(validator.validate(any())).thenReturn(Set.of());

        doThrow(new IllegalArgumentException("User already exists"))
                .when(authService)
                .register(eq("vika"), eq("123"), eq(Role.USER));

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_CONFLICT);

        ErrorResponse body = read(ErrorResponse.class);
        assertTrue(body.getMessage().contains("User already exists"));
    }

    @Test
    void register_ok_no_current_user_returns_created() throws Exception {
        when(req.getPathInfo()).thenReturn("/register");
        when(req.getInputStream()).thenReturn(json("""
                {"username":"vika", "password":"123"}
                """));

        when(validator.validate(any())).thenReturn(Set.of());

        when(authService.current()).thenReturn(Optional.empty());

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_CREATED);

        AuthResponseDto dto = read(AuthResponseDto.class);
        assertNull(dto.getId());
        assertEquals("vika", dto.getUsername());
        assertEquals(Role.USER, dto.getRole());
    }

    @Test
    void register_ok_with_current_user_returns_created() throws Exception {
        when(req.getPathInfo()).thenReturn("/register");
        when(req.getInputStream()).thenReturn(json("""
                {"username":"vika", "password":"123"}
                """));

        when(validator.validate(any(AuthRegisterRequestDto.class))).thenReturn(Set.of());

        User u = new User(10L, "vika", "pass", Role.USER);

        when(authService.current()).thenReturn(Optional.of(u));

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_CREATED);

        AuthResponseDto dto = read(AuthResponseDto.class);

        assertEquals(10L, dto.getId());
        assertEquals("vika", dto.getUsername());
        assertEquals(Role.USER, dto.getRole());
    }
}