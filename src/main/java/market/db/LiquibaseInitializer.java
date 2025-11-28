package market.db;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Инициализатор Liquibase для выполнения миграций при старте приложения.
 */
@Component
public class LiquibaseInitializer {

    private final DataSource dataSource;
    private final Environment env;

    public LiquibaseInitializer(DataSource dataSource, Environment env) {
        this.dataSource = dataSource;
        this.env = env;
    }

    @PostConstruct
    public void init() {
        MigrationRunner.runMigrations(
                dataSource,
                env.getProperty("liquibase.changelog"),
                env.getProperty("liquibase.defaultSchema"),
                env.getProperty("liquibase.serviceSchema")
        );
    }
}
