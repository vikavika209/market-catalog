package market.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Конфигурация Web MVC слоя приложения.
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {
        "market.controller.api.web"
})
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public OpenAPI marketOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Market Catalog API")
                        .version("1.0.0")
                        .description("REST API для работы с каталогом товаров"));
    }
}