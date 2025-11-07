
package market.repo;
import market.domain.Product;
import java.util.*;
public interface ProductRepository {
    Product save(Product p);
    Optional<Product> findById(long id);
    boolean deleteById(long id);
    List<Product> findAll();
    long nextId();
    void load();
    void flush();
}
