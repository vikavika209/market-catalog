package market.repo.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import market.db.MigrationRunner;
import market.domain.Category;
import market.domain.Product;
import market.repo.ProductRepository;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductRepositoryJdbcTest {
    private static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("marketdb")
                    .withUsername("app_user")
                    .withPassword("app_password");

    private DataSource ds;
    private ProductRepository repo;

    @BeforeAll
    void start() {
        PG.start();
        String url = PG.getJdbcUrl();
        MigrationRunner.runMigrations(url, PG.getUsername(), PG.getPassword(),
                "db/changelog/db.changelog-master.yaml",
                "market",
                "meta");

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        ds = new HikariDataSource(cfg);

        repo = new ProductRepositoryJdbc(ds);
    }

    @AfterAll
    void stop() {
        PG.stop();
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