package market.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Загружает конфигурацию приложения из файла {@code application.properties},
 * расположенного в classpath ({@code src/main/resources}).
 * <p>
 * Класс предоставляет удобные методы для доступа к параметрам подключения к БД,
 * настройкам Liquibase и дополнительным параметрам.
 * <p>
 * Используется как простой механизм конфигурации без Spring.
 */
public class AppConfig {

    /** Хранилище всех загруженных свойств. */
    private final Properties props = new Properties();

    /**
     * Создаёт новый экземпляр конфигурации и загружает файл
     * {@code application.properties} из classpath.
     *
     * @throws RuntimeException если файл отсутствует или произошла ошибка чтения
     */
    public AppConfig() {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (in == null) {
                throw new RuntimeException("Файл application.properties не найден");
            }

            props.load(in);

        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки конфигурации: " + e.getMessage());
        }
    }

    /**
     * Возвращает значение свойства по ключу.
     *
     * @param key имя параметра
     * @return значение или {@code null}, если параметр отсутствует
     */
    public String get(String key) {
        return props.getProperty(key);
    }

    /** @return хост PostgreSQL */
    public String dbHost() {
        return get("db.host");
    }

    /** @return порт PostgreSQL */
    public int dbPort() {
        return Integer.parseInt(get("db.port"));
    }

    /** @return имя базы данных */
    public String dbName() {
        return get("db.name");
    }

    /** @return имя пользователя БД */
    public String dbUser() {
        return get("db.user");
    }

    /** @return пароль пользователя БД */
    public String dbPassword() {
        return get("db.password");
    }

    /** @return путь к Liquibase changelog-файлу */
    public String liquibaseChangelog() {
        return get("liquibase.changelog");
    }

    /** @return схема, в которой создаются бизнес-таблицы */
    public String liquibaseDefaultSchema() {
        return get("liquibase.defaultSchema");
    }

    /** @return схема, в которой хранятся служебные таблицы Liquibase */
    public String liquibaseServiceSchema() {
        return get("liquibase.serviceSchema");
    }

    /** @return размер LRU-кэша для сервисов */
    public int cacheSize() {
        return Integer.parseInt(get("cache.size"));
    }

    /**
     * Формирует JDBC URL для подключения к PostgreSQL.
     *
     * @return строка вида {@code jdbc:postgresql://host:port/dbname}
     */
    public String jdbcUrl() {
        return "jdbc:postgresql://%s:%d/%s".formatted(
                dbHost(), dbPort(), dbName()
        );
    }
}

