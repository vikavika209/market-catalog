package market.web;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import market.aop.AopProxyFactory;
import market.config.AppConfig;
import market.db.MigrationRunner;
import market.repo.AuditRepository;
import market.repo.ProductRepository;
import market.repo.UserRepository;
import market.repo.jdbc.AuditRepositoryJdbc;
import market.repo.jdbc.ProductRepositoryJdbc;
import market.repo.jdbc.UserRepositoryJdbc;
import market.service.*;

import javax.sql.DataSource;

public class AppInitializer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext ctx = sce.getServletContext();

        AppConfig cfg = new AppConfig();

        String url = cfg.get("db.url");
        String user = cfg.get("db.user");
        String pass = cfg.get("db.password");

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(url);
        hc.setUsername(user);
        hc.setPassword(pass);
        DataSource ds = new HikariDataSource(hc);

        MigrationRunner.runMigrations(
                ds,
                cfg.get("liquibase.changelog"),
                cfg.get("liquibase.defaultSchema"),
                cfg.get("liquibase.serviceSchema")
        );

        ProductRepository productRepo = new ProductRepositoryJdbc(ds);
        UserRepository userRepo = new UserRepositoryJdbc(ds);
        AuditRepository auditRepo = new AuditRepositoryJdbc(ds);

        MetricsService metricsRaw = new MetricsServiceImpl();
        CatalogService catalogRaw = new CatalogServiceImpl(productRepo, metricsRaw, cfg.getInt("cache.size", 64));
        AuthService authRaw = new AuthServiceImpl(userRepo);
        AuditService auditService = new AuditServiceImpl(auditRepo);

        CatalogService catalog = AopProxyFactory.createProxy(
                catalogRaw,
                CatalogService.class,
                auditService,
                () -> "-"
        );

        AuthService auth = AopProxyFactory.createProxy(
                authRaw,
                AuthService.class,
                auditService,
                () -> "-"
        );

        ctx.setAttribute("catalogService", catalog);
        ctx.setAttribute("authService", auth);
        ctx.setAttribute("metricsService", metricsRaw);
        ctx.setAttribute("auditService", auditService);
        ctx.setAttribute("dataSource", ds);

        System.out.println("App initialized successfully");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        Object ds = ctx.getAttribute("dataSource");
        if (ds instanceof HikariDataSource hikari) {
            hikari.close();
        }
        System.out.println("App stopped");
    }
}
