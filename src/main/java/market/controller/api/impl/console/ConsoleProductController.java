package market.controller.api.impl.console;

import market.controller.api.ProductController;
import market.domain.Category;
import market.domain.Product;
import market.service.CatalogService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Реализация {@link ProductController}, предназначенная для работы
 * через консольный пользовательский интерфейс.
 * <p>
 * Данный контроллер является адаптером между UI-слоем (консоль)
 * и бизнес-логикой, инкапсулированной в {@link CatalogService}.
 * Он не содержит логики работы с товарами сам по себе — всё делегируется
 * сервисному слою.
 * <p>
 * Позволяет легко заменить консоль на REST API, не переписывая бизнес-логику.
 */
@Component
public class ConsoleProductController implements ProductController {
    private final CatalogService catalog;

    public ConsoleProductController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @Override
    public Product create(Product p) {
        return catalog.create(p);
    }

    @Override
    public Product update(Product p) {
        return catalog.update(p);
    }

    @Override
    public boolean delete(long id) {
        return catalog.delete(id);
    }

    @Override
    public Optional<Product> get(long id) {
        return catalog.get(id);
    }

    @Override
    public List<Product> list(int page, int size) {
        return catalog.paginate(catalog.listAll(), page, size);
    }

    @Override
    public List<Product> search(String q,
                                String brand,
                                Category category,
                                Double min,
                                Double max,
                                Boolean onlyActive,
                                int page,
                                int size
    ) {
        var res = catalog.search(q, brand, category, min, max, onlyActive);
        return catalog.paginate(res, page, size);
    }

    @Override
    public void persist() throws IOException {
        catalog.persist();
    }

    @Override
    public List<Product> paginate(List<Product> list, int page, int size) {
        return catalog.paginate(list, page, size);
    }
}
