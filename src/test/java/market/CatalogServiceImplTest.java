package market;


import market.cache.LRUCache;
import market.domain.Category;
import market.domain.Product;
import market.repo.InMemoryProductRepository;
import market.service.CatalogServiceImpl;
import market.service.MetricsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CatalogServiceImplTest {
    private CatalogServiceImpl service;

    @BeforeEach
    void setup() throws IOException {
        InMemoryProductRepository repo = new InMemoryProductRepository();
        MetricsServiceImpl metrics = new MetricsServiceImpl();
        LRUCache<String, List<Long>> cache = new LRUCache<>(100);
        service = new CatalogServiceImpl(repo, metrics, cache);
        service.create(new Product(0L, "iPhone 14", "Apple", Category.ELECTRONICS, 999.0, "Smartphone"));
        service.create(new Product(0L, "MacBook Air", "Apple", Category.ELECTRONICS, 1299.0, "Laptop"));
        service.create(new Product(0L, "Running Shoes", "Nike", Category.SPORTS, 120.0, "Shoes"));
        service.create(new Product(0L, "Coffee", "Lavazza", Category.FOOD, 8.5, "Beans"));
    }

    @Test
    void searchByBrand() {
        List<Product> res = service.search(null, "apple", null, null, null, true);
        assertEquals(2, res.size());
    }

    @Test
    void priceRange() {
        List<Product> res = service.search(null, null, null, 100.0, 1000.0, true);
        assertEquals(2, res.size());
    }

    @Test
    void pagination() {
        List<Product> all = service.listAll();
        List<Product> page0 = service.paginate(all, 0, 2);
        List<Product> page1 = service.paginate(all, 1, 2);
        List<Product> page2 = service.paginate(all, 2, 2);
        assertEquals(2, page0.size());
        assertEquals(2, page1.size());
        assertEquals(0, page2.size());
        assertNotEquals(page0.get(0).getId(), page1.get(0).getId());
    }
}
