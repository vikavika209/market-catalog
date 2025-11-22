package market.web;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;

import java.io.IOException;
import java.util.*;

/**
 * Утилитарный класс для вывода ошибок валидации в виде JSON.
 */
public class ValidationUtil {

    private ValidationUtil() {
    }

    public static void writeValidationErrors(HttpServletResponse resp, Set<? extends ConstraintViolation<?>> violations) throws IOException {

        List<Map<String, String>> errors = new ArrayList<>();

        for (ConstraintViolation<?> v : violations) {
            Map<String, String> err = new LinkedHashMap<>();
            err.put("field", v.getPropertyPath().toString());
            err.put("message", v.getMessage());
            errors.add(err);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errors", errors);

        resp.setContentType("application/json");
        JsonUtil.mapper().writeValue(resp.getOutputStream(), body);
    }
}
