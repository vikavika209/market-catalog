package market.controller.api.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import market.domain.Category;
import market.domain.Product;
import market.dto.product.ProductRequestDto;
import market.dto.product.ProductResponseDto;
import market.mapper.ProductMapper;
import market.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductRestControllerTest {

    @Mock
    private CatalogService catalogService;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductRestController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void searchOrList_success() throws Exception {
        Product p1 = new Product();
        p1.setId(1L);
        p1.setName("Phone");
        p1.setBrand("BrandX");
        p1.setCategory(Category.ELECTRONICS);
        p1.setPrice(300.0);

        Product p2 = new Product();
        p2.setId(2L);
        p2.setName("Laptop");
        p2.setBrand("BrandY");
        p2.setCategory(Category.ELECTRONICS);
        p2.setPrice(900.0);

        List<Product> found = List.of(p1, p2);
        List<Product> pageSlice = found;

        ProductResponseDto dto1 =
                new ProductResponseDto(1L, "Phone", "BrandX", "ELECTRONICS", 300.0, "desc1", true);
        ProductResponseDto dto2 =
                new ProductResponseDto(2L, "Laptop", "BrandY", "ELECTRONICS", 900.0, "desc2", true);

        when(catalogService.search(
                eq("phone"),
                eq("brand"),
                eq(Category.ELECTRONICS),
                eq(100.0),
                eq(1000.0),
                eq(true)
        )).thenReturn(found);

        when(catalogService.paginate(found, 0, 20)).thenReturn(pageSlice);

        when(productMapper.toDto(p1)).thenReturn(dto1);
        when(productMapper.toDto(p2)).thenReturn(dto2);

        mockMvc.perform(get("/api/products")
                        .param("q", " phone ")
                        .param("brand", " brand ")
                        .param("category", "electronics")
                        .param("minPrice", "100")
                        .param("maxPrice", "1000")
                        .param("onlyActive", "true")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Phone"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("Laptop"));
    }

    @Test
    void searchOrList_unknownCategory_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("category", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown category: UNKNOWN"));
    }

    @Test
    void getById_found() throws Exception {
        Product p = new Product();
        p.setId(10L);
        p.setName("TV");
        p.setBrand("BrandTV");
        p.setCategory(Category.ELECTRONICS);
        p.setPrice(500.0);

        ProductResponseDto dto =
                new ProductResponseDto(10L, "TV", "BrandTV", "ELECTRONICS", 500.0, "desc", true);

        when(catalogService.get(10L)).thenReturn(Optional.of(p));
        when(productMapper.toDto(p)).thenReturn(dto);

        mockMvc.perform(get("/api/products/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("TV"))
                .andExpect(jsonPath("$.brand").value("BrandTV"));
    }

    @Test
    void getById_notFound() throws Exception {
        when(catalogService.get(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found: id=99"));
    }

    @Test
    void create_success() throws Exception {
        String json = """
                {
                  "name": "Phone",
                  "brand": "BrandX",
                  "category": "ELECTRONICS",
                  "price": 999.0,
                  "description": "Nice phone",
                  "active": true
                }
                """;

        Product toSave = new Product();
        Product saved = new Product();
        saved.setId(5L);
        saved.setName("Phone");
        saved.setBrand("BrandX");
        saved.setCategory(Category.ELECTRONICS);
        saved.setPrice(999.0);
        saved.setDescription("Nice phone");
        saved.setActive(true);

        ProductResponseDto responseDto =
                new ProductResponseDto(5L, "Phone", "BrandX", "ELECTRONICS", 999.0, "Nice phone", true);

        when(productMapper.toEntity(any(ProductRequestDto.class))).thenReturn(toSave);
        when(catalogService.create(toSave)).thenReturn(saved);
        when(productMapper.toDto(saved)).thenReturn(responseDto);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.name").value("Phone"))
                .andExpect(jsonPath("$.brand").value("BrandX"));
    }

    @Test
    void update_success() throws Exception {
        String json = """
                {
                  "name": "New Name",
                  "brand": "New Brand",
                  "category": "ELECTRONICS",
                  "price": 1500.0,
                  "description": "Updated",
                  "active": false
                }
                """;

        Long id = 7L;

        Product existing = new Product();
        existing.setId(id);
        existing.setName("Old");
        existing.setBrand("OldBrand");
        existing.setCategory(Category.ELECTRONICS);
        existing.setPrice(1000.0);
        existing.setDescription("Old desc");
        existing.setActive(true);

        Product updated = new Product();
        updated.setId(id);
        updated.setName("New Name");
        updated.setBrand("New Brand");
        updated.setCategory(Category.ELECTRONICS);
        updated.setPrice(1500.0);
        updated.setDescription("Updated");
        updated.setActive(false);

        ProductResponseDto respDto =
                new ProductResponseDto(id, "New Name", "New Brand", "ELECTRONICS", 1500.0, "Updated", false);

        when(catalogService.get(id)).thenReturn(Optional.of(existing));
        doNothing().when(productMapper).updateEntity(any(ProductRequestDto.class), eq(existing));
        when(catalogService.update(existing)).thenReturn(updated);
        when(productMapper.toDto(updated)).thenReturn(respDto);

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.brand").value("New Brand"));
    }

    @Test
    void update_notFound() throws Exception {
        Long id = 123L;

        when(catalogService.get(id)).thenReturn(Optional.empty());

        String json = """
                {
                  "name": "X",
                  "brand": "Y",
                  "category": "ELECTRONICS",
                  "price": 10.0,
                  "description": "Z",
                  "active": true
                }
                """;

        mockMvc.perform(put("/api/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found: id=" + id));
    }

    @Test
    void delete_success() throws Exception {
        Long id = 50L;
        when(catalogService.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound() throws Exception {
        Long id = 51L;
        when(catalogService.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found: id=" + id));
    }

}