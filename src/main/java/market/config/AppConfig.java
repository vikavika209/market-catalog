package market.config;

import market.exception.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Универсальный загрузчик конфигурации.
 * <p>
 * Назначение класса — читать application.properties и предоставлять доступ
 * к параметрам через метод get(...).
 */
public class AppConfig {

    /**
     * Имя файла конфигурации в classpath
     */
    private static final String CONFIG_FILE = "application.properties";

    private final Properties props = new Properties();

    /**
     * Загружает файл application.properties из classpath.
     *
     * @throws ConfigurationException если файл не найден или повреждён
     */
    public AppConfig() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {

            if (in == null) {
                throw new ConfigurationException("Файл конфигурации '" + CONFIG_FILE + "' не найден в classpath");
            }

            props.load(in);

        } catch (IOException e) {
            throw new ConfigurationException("Ошибка загрузки конфигурации: " + e.getMessage(), e);
        }
    }

    /**
     * Получить значение параметра по ключу.
     *
     * @param key ключ параметра
     * @return строковое значение или null, если ключ отсутствует
     */
    public String get(String key) {
        return props.getProperty(key);
    }

    /**
     * Получить параметр или дефолтное значение.
     *
     * @param key          ключ
     * @param defaultValue значение по умолчанию
     * @return значение из конфигурации или defaultValue
     */
    public String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }


    /**
     * Получить числовой параметр с дефолтом.
     */
    public int getInt(String key, int defaultValue) {
        try {
            String value = get(key);
            return (value == null) ? defaultValue : Integer.parseInt(value);
        } catch (Exception e) {
            throw new ConfigurationException("Ожидалось целое число в параметре '" + key + "'", e);
        }
    }
}


