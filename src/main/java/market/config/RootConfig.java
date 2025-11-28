package market.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import market.cache.LRUCache;
import market.db.MigrationRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.List;

/**
 * Конфигурация Root Application Context
 * Класс отвечает за бины приложения
 */
@Configuration
@ComponentScan(basePackages = "market")
@PropertySource(
        value = "classpath:application.yml",
        factory = YamlPropertySourceFactory.class
)
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class RootConfig {

    private final Environment env;

    public RootConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(env.getProperty("db.url"));
        hc.setUsername(env.getProperty("db.user"));
        hc.setPassword(env.getProperty("db.password"));
        return new HikariDataSource(hc);
    }

    @Bean
    public LRUCache<String, List<Long>> productSearchCache() {
        int size = env.getProperty("cache.size", Integer.class, 64);
        return new LRUCache<>(size);
    }

    @PostConstruct
    public void runMigrations() {
        MigrationRunner.runMigrations(
                dataSource(),
                env.getProperty("liquibase.changelog"),
                env.getProperty("liquibase.defaultSchema"),
                env.getProperty("liquibase.serviceSchema")
        );
    }
}
