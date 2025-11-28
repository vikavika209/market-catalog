package market.controller.api.web;

import jakarta.validation.Valid;
import market.domain.Category;
import market.domain.Product;
import market.dto.ErrorResponse;
import market.dto.product.ProductRequestDto;
import market.dto.product.ProductResponseDto;
import market.mapper.ProductMapper;
import market.service.CatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-контроллер для управления товарами каталога.
 * <p>Предоставляет CRUD-операции и возможность поиска/фильтрации товаров.
 *
 * <h3>Эндпоинты:</h3>
 * <ul>
 *     <li><b>GET /api/products</b> — список товаров или поиск с фильтрами</li>
 *     <li><b>GET /api/products/{id}</b> — получение товара по ID</li>
 *     <li><b>POST /api/products</b> — создание товара</li>
 *     <li><b>PUT /api/products/{id}</b> — обновление товара</li>
 *     <li><b>DELETE /api/products/{id}</b> — удаление товара</li>
 * </p>
 */
@RestController
@RequestMapping("/api/products")
public class ProductRestController {

    private final CatalogService catalogService;
    private final ProductMapper productMapper;

    public ProductRestController(CatalogService catalogService, ProductMapper productMapper) {
        this.catalogService = catalogService;
        this.productMapper = productMapper;
    }

    /**
     * Получает список товаров или выполняет поиск с фильтрами.
     * @param q           часть названия или описания
     * @param brand       часть бренда
     * @param categoryStr категория (в строковом виде)
     * @param minPrice    минимальная цена
     * @param maxPrice    максимальная цена
     * @param onlyActive  фильтр по активности
     * @param page        номер страницы
     * @param size        количество элементов на странице
     * @return список подходящих товаров (DTO)
     */
    @GetMapping
    public ResponseEntity<?> searchOrList(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "category", required = false) String categoryStr,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "onlyActive", required = false) Boolean onlyActive,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Category category = null;
        if (categoryStr != null && !categoryStr.isBlank()) {
            try {
                category = Category.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity
                        .badRequest()
                        .body(new ErrorResponse("Unknown category: " + categoryStr));
            }
        }

        List<Product> products = catalogService.search(
                trimOrNull(q),
                trimOrNull(brand),
                category,
                minPrice,
                maxPrice,
                onlyActive
        );

        List<Product> pageSlice = catalogService.paginate(products, page, size);
        List<ProductResponseDto> dtos = pageSlice.stream()
                .map(productMapper::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Возвращает товар по идентификатору.
     * @param id идентификатор товара
     * @return 200 OK — товар найден;<br>
     * 404 Not Found — товар отсутствует.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") Long id) {
        var opt = catalogService.get(id);
        if (opt.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Product not found: id=" + id));
        }
        ProductResponseDto dto = productMapper.toDto(opt.get());
        return ResponseEntity.ok(dto);
    }

    /**
     * Создаёт новый товар.
     * @param dto JSON-данные с параметрами товара
     * @return 201 Created — товар успешно создан;<br>
     * 400 Bad Request — ошибки валидации.
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ProductRequestDto dto) {
        Product product = productMapper.toEntity(dto);
        Product saved = catalogService.create(product);
        ProductResponseDto responseDto = productMapper.toDto(saved);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(responseDto);
    }

    /**
     * Обновляет существующий товар по ID.
     * @param id  идентификатор товара
     * @param dto данные для обновления
     * @return 200 OK — товар обновлён;<br>
     * 404 Not Found — товар отсутствует;<br>
     * 400 Bad Request — ошибки валидации.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id,
                                    @Valid @RequestBody ProductRequestDto dto) {

        var existing = catalogService.get(id);
        if (existing.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Product not found: id=" + id));
        }

        Product toUpdate = existing.get();
        productMapper.updateEntity(dto, toUpdate);
        Product updated = catalogService.update(toUpdate);
        ProductResponseDto respDto = productMapper.toDto(updated);

        return ResponseEntity.ok(respDto);
    }

    /**
     * Удаляет товар по ID.
     * @param id идентификатор товара
     * @return 204 No Content — если удалено успешно;<br>
     * 404 Not Found — если товара нет.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        boolean deleted = catalogService.delete(id);
        if (!deleted) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Product not found: id=" + id));
        }
        return ResponseEntity.noContent().build();
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
