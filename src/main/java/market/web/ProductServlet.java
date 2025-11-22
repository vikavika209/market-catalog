package market.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import market.domain.Category;
import market.domain.Product;
import market.dto.ErrorResponse;
import market.dto.product.ProductRequestDto;
import market.dto.product.ProductResponseDto;
import market.mapper.ProductMapper;
import market.service.CatalogService;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * REST-сервлет для работы с товарами.
 * Base URL: /api/products
 * Поддерживаемые операции:
 * - GET  /api/products                — список / поиск с фильтрами
 * - GET  /api/products/{id}           — получить товар по id
 * - POST /api/products                — создать товар (JSON в теле)
 * - PUT  /api/products/{id}           — обновить товар (JSON в теле)
 * - DELETE /api/products/{id}         — удалить товар
 * Все ответы и запросы — в формате JSON.
 */
public class ProductServlet extends HttpServlet {
    private CatalogService catalogService;
    private ProductMapper productMapper;
    private ObjectMapper objectMapper;
    private Validator validator;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        var ctx = config.getServletContext();

        this.catalogService = (CatalogService) ctx.getAttribute("catalogService");
        if (catalogService == null) {
            throw new IllegalStateException("CatalogService not found in ServletContext");
        }

        this.productMapper = ProductMapper.INSTANCE;
        this.objectMapper = new ObjectMapper();
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        if (path == null || "/".equals(path)) {
            handleSearchOrList(req, resp);
        } else {
            Long id = parseIdFromPath(path);
            if (id == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeJson(resp, error("Invalid id in path"));
                return;
            }
            handleGetById(id, resp);
        }
    }

    private void handleGetById(Long id, HttpServletResponse resp) throws IOException {
        var opt = catalogService.get(id);
        if (opt.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeJson(resp, error("Product not found: id=" + id));
            return;
        }
        Product product = opt.get();
        ProductResponseDto dto = productMapper.toDto(product);
        resp.setStatus(HttpServletResponse.SC_OK);
        writeJson(resp, dto);
    }

    private void handleSearchOrList(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String q = trimOrNull(req.getParameter("q"));
        String brand = trimOrNull(req.getParameter("brand"));
        String catStr = trimOrNull(req.getParameter("category"));
        String minStr = trimOrNull(req.getParameter("minPrice"));
        String maxStr = trimOrNull(req.getParameter("maxPrice"));
        String actStr = trimOrNull(req.getParameter("onlyActive"));
        String pageStr = trimOrNull(req.getParameter("page"));
        String sizeStr = trimOrNull(req.getParameter("size"));

        Category category = null;
        if (catStr != null) {
            try {
                category = Category.valueOf(catStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                writeJson(resp, error("Unknown category: " + catStr));
                return;
            }
        }

        Double min = null;
        Double max = null;
        Boolean onlyActive = null;
        int page = 0;
        int size = 20;

        try {
            if (minStr != null) min = Double.valueOf(minStr);
            if (maxStr != null) max = Double.valueOf(maxStr);
            if (actStr != null) onlyActive = Boolean.valueOf(actStr);
            if (pageStr != null) page = Integer.parseInt(pageStr);
            if (sizeStr != null) size = Integer.parseInt(sizeStr);
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, error("Invalid numeric parameter: " + e.getMessage()));
            return;
        }

        List<Product> products = catalogService.search(q, brand, category, min, max, onlyActive);
        List<Product> pageSlice = catalogService.paginate(products, page, size);

        List<ProductResponseDto> dtos = pageSlice.stream()
                .map(productMapper::toDto)
                .toList();

        resp.setStatus(HttpServletResponse.SC_OK);
        writeJson(resp, dtos);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ProductRequestDto dto = readAndValidateDto(req, resp);
        if (dto == null) {
            return;
        }

        Product product = productMapper.toEntity(dto);
        Product saved = catalogService.create(product);
        ProductResponseDto responseDto = productMapper.toDto(saved);

        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.setHeader("Location", req.getRequestURI() + "/" + saved.getId());
        writeJson(resp, responseDto);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        Long id = parseIdFromPath(path);
        if (id == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, error("Invalid id in path"));
            return;
        }

        ProductRequestDto dto = readAndValidateDto(req, resp);
        if (dto == null) {
            return;
        }

        var existing = catalogService.get(id);
        if (existing.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeJson(resp, error("Product not found: id=" + id));
            return;
        }

        Product toUpdate = existing.get();
        productMapper.updateEntity(dto, toUpdate);

        Product updated = catalogService.update(toUpdate);
        ProductResponseDto respDto = productMapper.toDto(updated);

        resp.setStatus(HttpServletResponse.SC_OK);
        writeJson(resp, respDto);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        Long id = parseIdFromPath(path);
        if (id == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, error("Invalid id in path"));
            return;
        }

        boolean deleted = catalogService.delete(id);
        if (!deleted) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeJson(resp, error("Product not found: id=" + id));
            return;
        }

        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private Long parseIdFromPath(String pathInfo) {
        if (pathInfo == null) return null;
        String trimmed = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        if (trimmed.isBlank()) return null;
        try {
            return Long.valueOf(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void writeJson(HttpServletResponse resp, Object body) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(resp.getOutputStream(), body);
    }

    private ErrorResponse error(String msg) {
        return new ErrorResponse(msg);
    }

    private ProductRequestDto readAndValidateDto(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ProductRequestDto dto;
        try {
            dto = objectMapper.readValue(req.getInputStream(), ProductRequestDto.class);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, error("Invalid JSON: " + e.getMessage()));
            return null;
        }

        Set<ConstraintViolation<ProductRequestDto>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ValidationUtil.writeValidationErrors(resp, violations);
            return null;
        }

        return dto;
    }
}
