package market.db;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.core.PostgresDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import market.exception.MigrationException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Утилитарный класс для выполнения миграций базы данных через Liquibase.
 */
public class MigrationRunner {

    /**
     * Запускает миграции Liquibase, используя переданный DataSource.
     *
     * @param dataSource    источник подключений к БД
     * @param changeLog     путь к Liquibase-changelog
     * @param defaultSchema схема, в которой хранятся таблицы доменной модели
     * @param serviceSchema схема для служебных таблиц Liquibase
     * @throws MigrationException если миграция завершилась с ошибкой
     */
    public static void runMigrations(DataSource dataSource, String changeLog, String defaultSchema, String serviceSchema) {
        try (Connection c = dataSource.getConnection()) {

            // Создание служебной схемы Liquibase, если её нет
            try (Statement st = c.createStatement()) {
                st.execute("CREATE SCHEMA IF NOT EXISTS " + serviceSchema);
            }

            Database database = new PostgresDatabase();
            database.setDefaultSchemaName(defaultSchema);
            database.setLiquibaseSchemaName(serviceSchema);
            database.setConnection(new JdbcConnection(c));

            Liquibase liquibase = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), database);

            liquibase.update((String) null);

        } catch (Exception e) {
            throw new MigrationException("Liquibase migration failed", e);
        }
    }
}
