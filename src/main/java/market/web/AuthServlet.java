package market.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import market.domain.Role;
import market.domain.User;
import market.dto.ErrorResponse;
import market.dto.auth.AuthLoginRequestDto;
import market.dto.auth.AuthRegisterRequestDto;
import market.dto.auth.AuthResponseDto;
import market.repo.UserRepository;
import market.repo.jdbc.UserRepositoryJdbc;
import market.service.AuthService;
import market.service.AuthServiceImpl;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Set;

/**
 * HTTP-сервлет для аутентификации и регистрации.
 * <p>
 * Обрабатывает JSON-запросы:
 * POST /api/auth/login
 * POST /api/auth/register
 */
public class AuthServlet extends HttpServlet {

    private ObjectMapper mapper;
    private Validator validator;
    private AuthService authService;

    @Override
    public void init() throws ServletException {
        super.init();
        this.mapper = new ObjectMapper();
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();

        Object dsAttr = getServletContext().getAttribute("ds");
        if (dsAttr instanceof DataSource ds) {
            UserRepository userRepo = new UserRepositoryJdbc(ds);
            this.authService = new AuthServiceImpl(userRepo);
        } else {
            throw new ServletException("DataSource (атрибут 'ds') не найден в ServletContext");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String path = req.getPathInfo();
        if (path == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeError(resp, "Unknown auth endpoint");
            return;
        }

        switch (path) {
            case "/login" -> handleLogin(req, resp);
            case "/register" -> handleRegister(req, resp);
            default -> {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                writeError(resp, "Unknown auth endpoint: " + path);
            }
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AuthLoginRequestDto dto;
        try {
            dto = mapper.readValue(req.getInputStream(), AuthLoginRequestDto.class);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Invalid JSON for login");
            return;
        }

        Set<ConstraintViolation<AuthLoginRequestDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ValidationUtil.writeValidationErrors(resp, violations);
            return;
        }

        var userOpt = authService.login(dto.getUsername(), dto.getPassword());
        if (userOpt.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeError(resp, "Invalid credentials");
            return;
        }

        User user = userOpt.get();
        AuthResponseDto responseDto =
                new AuthResponseDto(user.getId(), user.getUsername(), user.getRole());

        resp.setStatus(HttpServletResponse.SC_OK);
        mapper.writeValue(resp.getOutputStream(), responseDto);
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AuthRegisterRequestDto dto;
        try {
            dto = mapper.readValue(req.getInputStream(), AuthRegisterRequestDto.class);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeError(resp, "Invalid JSON for register");
            return;
        }

        Set<ConstraintViolation<AuthRegisterRequestDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ValidationUtil.writeValidationErrors(resp, violations);
            return;
        }

        try {
            authService.register(dto.getUsername(), dto.getPassword(), Role.USER);
        } catch (IllegalArgumentException ex) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            writeError(resp, ex.getMessage());
            return;
        }

        var userOpt = authService.current();
        if (userOpt.isEmpty()) {
            AuthResponseDto responseDto =
                    new AuthResponseDto(null, dto.getUsername(), Role.USER);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            mapper.writeValue(resp.getOutputStream(), responseDto);
            return;
        }

        User user = userOpt.get();
        AuthResponseDto responseDto =
                new AuthResponseDto(user.getId(), user.getUsername(), user.getRole());

        resp.setStatus(HttpServletResponse.SC_CREATED);
        mapper.writeValue(resp.getOutputStream(), responseDto);
    }

    private void writeError(HttpServletResponse resp, String message) throws IOException {
        mapper.writeValue(resp.getOutputStream(), new ErrorResponse(message));
    }
}