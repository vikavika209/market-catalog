package market.repo.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import market.db.MigrationRunner;
import market.domain.AuditAction;
import market.domain.AuditEvent;
import market.repo.AuditRepository;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditRepositoryJdbcTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("market_db")
                    .withUsername("market_user")
                    .withPassword("market_pass");

    private static DataSource dataSource;
    private static AuditRepository auditRepo;

    @BeforeAll
    static void start() {
        POSTGRES.start();

        MigrationRunner.runMigrations(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword(),
                "db/changelog/db.changelog-master.yaml",
                "market",
                "meta"
        );

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(POSTGRES.getJdbcUrl());
        cfg.setUsername(POSTGRES.getUsername());
        cfg.setPassword(POSTGRES.getPassword());
        cfg.setMaximumPoolSize(3);

        dataSource = new HikariDataSource(cfg);

        auditRepo = new AuditRepositoryJdbc(dataSource);
    }

    @AfterAll
    static void stop() {
        POSTGRES.stop();
    }

    @Test
    @Order(1)
    void save_shouldAssignIdAndPersistEvent() {
        AuditEvent e = new AuditEvent();
        e.setUsername("vika");
        e.setAction(AuditAction.LOGIN);
        e.setDetails("login from test");
        e.setTimestamp(LocalDateTime.now().minusMinutes(1));

        auditRepo.save(e);

        Assertions.assertNotNull(e.getId(), "ID должен быть установлен после save()");

        List<AuditEvent> all = auditRepo.findAll();
        Assertions.assertFalse(all.isEmpty(), "Список аудитов не должен быть пустым");
        Assertions.assertTrue(
                all.stream().anyMatch(a -> a.getId().equals(e.getId())),
                "Сохранённое событие должно находиться в findAll()"
        );
    }

    @Test
    @Order(2)
    void findByUsername_shouldReturnOnlyEventsForGivenUser() {
        AuditEvent e1 = new AuditEvent();
        e1.setUsername("user1");
        e1.setAction(AuditAction.CREATE);
        e1.setDetails("create product");
        e1.setTimestamp(LocalDateTime.now().minusMinutes(2));
        auditRepo.save(e1);

        AuditEvent e2 = new AuditEvent();
        e2.setUsername("user2");
        e2.setAction(AuditAction.DELETE);
        e2.setDetails("delete product");
        e2.setTimestamp(LocalDateTime.now().minusMinutes(1));
        auditRepo.save(e2);

        List<AuditEvent> user1Events = auditRepo.findByUsername("user1");
        Assertions.assertFalse(user1Events.isEmpty(), "Для user1 должны быть события");
        Assertions.assertTrue(
                user1Events.stream().allMatch(ev -> "user1".equals(ev.getUsername())),
                "Все события должны принадлежать user1"
        );

        List<AuditEvent> user2Events = auditRepo.findByUsername("user2");
        Assertions.assertFalse(user2Events.isEmpty(), "Для user2 должны быть события");
        Assertions.assertTrue(
                user2Events.stream().allMatch(ev -> "user2".equals(ev.getUsername())),
                "Все события должны принадлежать user2"
        );
    }

    @Test
    @Order(3)
    void findRecent_shouldRespectLimitAndOrder() {
        for (int i = 0; i < 5; i++) {
            AuditEvent e = new AuditEvent();
            e.setUsername("recent");
            e.setAction(AuditAction.UPDATE);
            e.setDetails("event #" + i);
            e.setTimestamp(LocalDateTime.now().minusSeconds(10 - i));
            auditRepo.save(e);
        }

        int limit = 3;
        List<AuditEvent> recent = auditRepo.findRecent(limit);

        Assertions.assertFalse(recent.isEmpty(), "Список последних событий не должен быть пустым");
        Assertions.assertTrue(recent.size() <= limit, "Размер списка не должен превышать limit");

        if (recent.size() > 1) {
            Assertions.assertTrue(
                    !recent.get(0).getTimestamp().isBefore(recent.get(recent.size() - 1).getTimestamp()),
                    "Первое событие должно быть не раньше последнего по времени"
            );
        }
    }

}