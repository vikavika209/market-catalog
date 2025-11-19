package market.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.database.core.PostgresDatabase;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Утилитарный класс для выполнения миграций базы данных через Liquibase.
 * <p>
 * Используется при старте приложения и в тестах (в том числе Testcontainers),
 * чтобы гарантировать, что схема БД, служебные таблицы Liquibase и данные
 * инициализации находятся в актуальном состоянии.
 * <p>
 * Данный класс:
 * <ul>
 *   <li>создаёт DataSource на основе переданных параметров;</li>
 *   <li>гарантирует существование служебной схемы для Liquibase;</li>
 *   <li>конфигурирует Liquibase на работу с PostgreSQL;</li>
 *   <li>запускает миграции из указанного changelog-файла;</li>
 *   <li>применяет миграции строго в заданном порядке;</li>
 * </ul>
 * <p>
 * Полностью независимый компонент, не требующий Spring или внешних фреймворков.
 */
public class MigrationRunner {

    /**
     * Запускает миграции Liquibase с указанными параметрами подключения.
     *
     * @param url           JDBC-URL к PostgreSQL
     * @param user          имя пользователя БД
     * @param pass          пароль пользователя БД
     * @param changeLog     путь к Liquibase-changelog
     * @param defaultSchema схема, в которой будут храниться таблицы проекта
     * @param serviceSchema схема для служебных таблиц Liquibase
     *
     * @throws RuntimeException если миграция завершилась с ошибкой
     *
     * <p><b>Примечания:</b></p>
     * <ul>
     *     <li><code>serviceSchema</code> используется для таблиц
     *         <i>DATABASECHANGELOG</i> и <i>DATABASECHANGELOGLOCK</i>;</li>
     *     <li>если служебная схема отсутствует — создаётся автоматически;</li>
     *     <li>соединение с БД создаётся через HikariCP, что обеспечивает
     *         предсказуемую и быструю работу даже в тестах;</li>
     * </ul>
     */
    public static void runMigrations(
            String url,
            String user,
            String pass,
            String changeLog,
            String defaultSchema,
            String serviceSchema
    ) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        DataSource ds = new HikariDataSource(cfg);

        try (Connection c = ds.getConnection()) {

            // Создание служебной схемы Liquibase, если её нет
            try (Statement st = c.createStatement()) {
                st.execute("CREATE SCHEMA IF NOT EXISTS " + serviceSchema);
            }

            // Настройка Liquibase для PostgreSQL
            Database database = new PostgresDatabase();
            database.setDefaultSchemaName(defaultSchema);
            database.setLiquibaseSchemaName(serviceSchema);
            database.setConnection(new JdbcConnection(c));
            Liquibase liquibase = new Liquibase(
                    changeLog,
                    new ClassLoaderResourceAccessor(),
                    database
            );

            // Применение всех миграций
            liquibase.update((String) null);

        } catch (Exception e) {
            throw new RuntimeException("Liquibase migration failed", e);
        }
    }
}
