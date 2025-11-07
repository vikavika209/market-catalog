
package market;
import market.domain.*;
import market.repo.InMemoryProductRepository;
import market.service.CatalogService;
import market.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
public class CatalogServiceTest {
    private CatalogService service;
    @BeforeEach
    void setup(){
        InMemoryProductRepository repo = new InMemoryProductRepository();
        MetricsService metrics = new MetricsService();
        service = new CatalogService(repo, metrics);
        service.create(new Product(0,"iPhone 14","Apple", Category.ELECTRONICS, 999.0,"Smartphone"));
        service.create(new Product(0,"MacBook Air","Apple", Category.ELECTRONICS, 1299.0,"Laptop"));
        service.create(new Product(0,"Running Shoes","Nike", Category.SPORTS, 120.0,"Shoes"));
        service.create(new Product(0,"Coffee","Lavazza", Category.FOOD, 8.5,"Beans"));
    }
    @Test
    void searchByBrand(){
        List<Product> res = service.search(null, "apple", null, null, null, true);
        assertEquals(2, res.size());
    }
    @Test
    void priceRange(){
        List<Product> res = service.search(null, null, null, 100.0, 1000.0, true);
        assertEquals(2, res.size());
    }
    @Test
    void pagination(){
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
