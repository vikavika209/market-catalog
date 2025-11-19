package market.controller.api.impl.console;

import market.controller.api.ProductController;
import market.domain.Category;
import market.domain.Product;
import market.service.CatalogService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ConsoleProductController implements ProductController {
    private final CatalogService catalog;

    public ConsoleProductController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @Override
    public Product create(Product p){
        return catalog.create(p);
    }

    @Override
    public Product update(Product p){
        return catalog.update(p);
    }

    @Override
    public boolean delete(long id){
        return catalog.delete(id);
    }
    @Override
    public Optional<Product> get(long id){
        return catalog.get(id);
    }

    @Override
    public List<Product> list(int page, int size){
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
    ){
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
