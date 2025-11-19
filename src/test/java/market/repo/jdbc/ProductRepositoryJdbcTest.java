package market.repo.jdbc;

import market.domain.Category;
import market.domain.Product;
import market.repo.ProductRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductRepositoryJdbcTest extends BaseJdbcTest {
    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("marketdb").withUsername("app_user").withPassword("app_password");

    private ProductRepository repo;

    @BeforeAll
    void initRepo() {
        repo = new ProductRepositoryJdbc(ds);
    }

    @Test
    void create_and_find() {
        Product p = new Product();
        p.setName("Test");
        p.setBrand("Brand");
        p.setCategory(Category.ELECTRONICS);
        p.setPrice(123.45);
        p.setActive(true);

        Product saved = repo.save(p);
        Assertions.assertNotNull(saved.getId());

        var found = repo.findById(saved.getId());
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("Test", found.get().getName());
    }
}