package market.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.lang.NonNull;

import java.util.Objects;
import java.util.Properties;

/**
 * Фабрика для загрузки YAML-файлов как источников свойств Spring.
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {
    @Override
    @NonNull
    public PropertySource<?> createPropertySource(
            String name,
            @NonNull
            EncodedResource resource) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource.getResource());
        Properties props = factory.getObject();
        return new PropertiesPropertySource(
                name != null ? name : Objects.requireNonNull(resource.getResource().getFilename()),
                props != null ? props : new Properties()
        );
    }
}