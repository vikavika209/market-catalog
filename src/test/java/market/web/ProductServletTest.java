package market.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import market.domain.Category;
import market.domain.Product;
import market.dto.ErrorResponse;
import market.dto.product.ProductRequestDto;
import market.dto.product.ProductResponseDto;
import market.mapper.ProductMapper;
import market.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServletTest {

    private ProductServlet servlet;

    @Mock
    private CatalogService catalogService;

    @Mock
    private Validator validator;

    @Mock
    private HttpServletRequest req;

    @Mock
    private HttpServletResponse resp;

    private ObjectMapper objectMapper;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setup() throws Exception {
        servlet = new ProductServlet();

        objectMapper = new ObjectMapper();
        responseBody = new ByteArrayOutputStream();

        inject(servlet, "catalogService", catalogService);
        inject(servlet, "validator", validator);
        inject(servlet, "objectMapper", objectMapper);
        inject(servlet, "productMapper", ProductMapper.INSTANCE);

        when(req.getParameter(anyString())).thenReturn(null);

        when(resp.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener listener) {

            }

            @Override
            public void write(int b) {
                responseBody.write(b);
            }
        });
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

    private <T> T readBody(Class<T> clazz) throws IOException {
        return objectMapper.readValue(responseBody.toByteArray(), clazz);
    }

    private <T> T readBody(TypeReference<T> type) throws IOException {
        return objectMapper.readValue(responseBody.toByteArray(), type);
    }

    @Test
    void init_throws_if_no_catalogService() {
        ServletConfig config = mock(ServletConfig.class);
        ServletContext ctx = mock(ServletContext.class);

        when(config.getServletContext()).thenReturn(ctx);
        when(ctx.getAttribute("catalogService")).thenReturn(null);

        ProductServlet servlet = new ProductServlet();

        assertThrows(IllegalStateException.class, () -> servlet.init(config));
    }

    @Test
    void init_ok_catalogService_injected() throws Exception {
        ServletConfig config = mock(ServletConfig.class);
        ServletContext ctx = mock(ServletContext.class);
        CatalogService cs = mock(CatalogService.class);

        when(config.getServletContext()).thenReturn(ctx);
        when(ctx.getAttribute("catalogService")).thenReturn(cs);

        ProductServlet servlet = new ProductServlet();

        inject(servlet, "catalogService", cs);


        var f = ProductServlet.class.getDeclaredField("catalogService");
        f.setAccessible(true);
        assertSame(cs, f.get(servlet));
    }

    @Test
    void doGet_returns400_if_id_invalid() throws Exception {
        when(req.getPathInfo()).thenReturn("/abc");

        servlet.doGet(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        ErrorResponse body = readBody(ErrorResponse.class);
        assertTrue(body.getMessage().contains("Invalid id"));
    }

    @Test
    void doGet_returns404_if_not_found() throws Exception {
        when(req.getPathInfo()).thenReturn("/100");
        when(catalogService.get(100L)).thenReturn(Optional.empty());

        servlet.doGet(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_NOT_FOUND);
        ErrorResponse body = readBody(ErrorResponse.class);
        assertTrue(body.getMessage().contains("Product not found"));
    }

    @Test
    void doGet_returns_product_if_found() throws Exception {
        when(req.getPathInfo()).thenReturn("/5");

        Product p = new Product();
        p.setId(5L);
        p.setName("A");
        p.setBrand("B");
        p.setCategory(Category.ELECTRONICS);
        p.setPrice(10);
        p.setDescription("desc");

        when(catalogService.get(5L)).thenReturn(Optional.of(p));

        servlet.doGet(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        ProductResponseDto dto = readBody(ProductResponseDto.class);

        assertEquals(5L, dto.getId());
        assertEquals("A", dto.getName());
    }

    @Test
    void doGet_list_returns400_unknown_category() throws Exception {
        when(req.getPathInfo()).thenReturn(null);
        when(req.getParameter("category")).thenReturn("badCat");

        servlet.doGet(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        ErrorResponse body = readBody(ErrorResponse.class);
        assertTrue(body.getMessage().contains("Unknown category"));
    }

    @Test
    void doGet_list_returns400_invalid_numeric() throws Exception {
        when(req.getPathInfo()).thenReturn(null);
        when(req.getParameter("minPrice")).thenReturn("oops");

        servlet.doGet(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        ErrorResponse body = readBody(ErrorResponse.class);
        assertTrue(body.getMessage().contains("Invalid numeric parameter"));
    }

    @Test
    void doGet_list_ok() throws Exception {
        when(req.getPathInfo()).thenReturn(null);

        Product p = new Product();
        p.setId(1L);
        p.setName("Phone");
        p.setBrand("Samsung");
        p.setCategory(Category.ELECTRONICS);
        p.setPrice(500);
        p.setDescription("desc");

        when(catalogService.search(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(p));
        when(catalogService.paginate(List.of(p), 0, 20))
                .thenReturn(List.of(p));

        servlet.doGet(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        List<ProductResponseDto> list = readBody(new TypeReference<>() {
        });
        assertEquals(1, list.size());
        assertEquals("Phone", list.getFirst().getName());
    }

    @Test
    void doPost_returns400_invalid_json() throws Exception {
        when(req.getInputStream()).thenReturn(json("bad-json"));

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        ErrorResponse body = readBody(ErrorResponse.class);
        assertTrue(body.getMessage().contains("Invalid JSON"));
    }

    @Test
    void doPost_returns400_validation() throws Exception {
        String json = """
                { "name": "A", "brand": "", "category": "ELECTRONICS", "price": 10 }
                """;

        when(req.getInputStream()).thenReturn(json(json));

        @SuppressWarnings("unchecked")
        ConstraintViolation<ProductRequestDto> v =
                (ConstraintViolation<ProductRequestDto>) mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("brand");
        when(v.getPropertyPath()).thenReturn(path);
        when(v.getMessage()).thenReturn("must not be empty");

        when(validator.validate(any(ProductRequestDto.class))).thenReturn(Set.of(v));

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);

        Map<String, Object> body = readBody(new TypeReference<>() {
        });

        @SuppressWarnings("unchecked")
        List<Map<String, String>> errors = (List<Map<String, String>>) body.get("errors");

        assertEquals(1, errors.size());
        assertEquals("brand", errors.getFirst().get("field"));
        assertEquals("must not be empty", errors.getFirst().get("message"));
    }

    @Test
    void doPost_ok() throws Exception {
        String json = """
                {
                  "name": "Test",
                  "brand": "Brand",
                  "category": "ELECTRONICS",
                  "price": 10,
                  "description": "desc"
                }
                """;

        when(req.getInputStream()).thenReturn(json(json));
        when(req.getRequestURI()).thenReturn("/api/products");
        when(validator.validate(any(ProductRequestDto.class))).thenReturn(Set.of());

        Product saved = new Product();
        saved.setId(99L);
        saved.setName("Test");
        saved.setBrand("Brand");
        saved.setCategory(Category.ELECTRONICS);
        saved.setPrice(10);

        when(catalogService.create(any())).thenReturn(saved);

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_CREATED);
        verify(resp).setHeader("Location", "/api/products/99");

        ProductResponseDto dto = readBody(ProductResponseDto.class);
        assertEquals(99L, dto.getId());
        assertEquals("Test", dto.getName());
    }

    @Test
    void doPut_returns400_invalid_id() throws Exception {
        when(req.getPathInfo()).thenReturn("/abc");

        servlet.doPut(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void doPut_returns404_not_found() throws Exception {
        when(req.getPathInfo()).thenReturn("/7");
        when(catalogService.get(7L)).thenReturn(Optional.empty());

        when(req.getInputStream()).thenReturn(json("{\"name\":\"A\"}"));
        when(validator.validate(any())).thenReturn(Set.of());

        servlet.doPut(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void doPut_ok() throws Exception {
        when(req.getPathInfo()).thenReturn("/7");

        String json = """
            {
              "name": "New",
              "brand": "Brand",
              "category": "ELECTRONICS",
              "price": 20,
              "description": "d",
              "active": true
            }
            """;

        when(req.getInputStream()).thenReturn(json(json));

        when(validator.validate(any(ProductRequestDto.class)))
                .thenReturn(Collections.emptySet());

        Product existing = new Product();
        existing.setId(7L);
        existing.setName("Old");
        existing.setBrand("Old");
        existing.setCategory(Category.ELECTRONICS);
        existing.setPrice(5);

        when(catalogService.get(7L)).thenReturn(Optional.of(existing));

        Product updated = new Product();
        updated.setId(7L);
        updated.setName("New");
        updated.setBrand("Brand");
        updated.setCategory(Category.ELECTRONICS);
        updated.setPrice(20);

        when(catalogService.update(any())).thenReturn(updated);

        inject(servlet, "validator", validator);

        servlet.doPut(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);

        ProductResponseDto dto = readBody(ProductResponseDto.class);
        assertEquals("New", dto.getName());
        assertEquals(20, dto.getPrice());
    }

    @Test
    void doDelete_returns400_invalid_id() throws Exception {
        when(req.getPathInfo()).thenReturn("/bad");

        servlet.doDelete(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    void doDelete_returns404_not_found() throws Exception {
        when(req.getPathInfo()).thenReturn("/3");
        when(catalogService.delete(3L)).thenReturn(false);

        servlet.doDelete(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void doDelete_ok() throws Exception {
        when(req.getPathInfo()).thenReturn("/3");
        when(catalogService.delete(3L)).thenReturn(true);

        servlet.doDelete(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_NO_CONTENT);

        assertEquals(0, responseBody.size());
    }
}