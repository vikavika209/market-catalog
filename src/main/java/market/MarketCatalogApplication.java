package market;

import com.pet.loggingspringbootstarter.logging.EnableLogging;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableLogging
@SpringBootApplication
public class MarketCatalogApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(MarketCatalogApplication.class, args);
    }
}
