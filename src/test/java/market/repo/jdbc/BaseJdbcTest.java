package market.repo.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import market.db.MigrationRunner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

public class BaseJdbcTest {

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("testdb").withUsername("test_user").withPassword("test_pass");

    protected static DataSource ds;

    @BeforeAll
    static void startContainer() {
        POSTGRES.start();

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(POSTGRES.getJdbcUrl());
        cfg.setUsername(POSTGRES.getUsername());
        cfg.setPassword(POSTGRES.getPassword());
        ds = new HikariDataSource(cfg);

        MigrationRunner.runMigrations(ds, "db/changelog/db.changelog-master.yaml", "market", "meta");
    }

    @AfterAll
    static void stopContainer() {
        POSTGRES.stop();
    }
}
