package market.service;

import com.pet.auditspringbootstarter.audit.Audited;
import com.pet.loggingspringbootstarter.logging.Logged;
import market.cache.LRUCache;
import market.domain.Category;
import market.domain.Product;
import market.repo.ProductRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Реализация сервиса каталога товаров.
 * Инкапсулирует бизнес-логику CRUD-операций, поиска, фильтрации и пагинации.
 * Работает поверх {@link ProductRepository} и использует {@link MetricsService}
 * для сбора метрик (время выполнения запросов, статистика кеша).
 */
@Service
public class CatalogServiceImpl implements CatalogService {
    private final ProductRepository repo;
    private final MetricsService metrics;
    private final LRUCache<String, List<Long>> cache;

    public CatalogServiceImpl(ProductRepository repo, MetricsService metrics, LRUCache<String, List<Long>> cache) {
        this.repo = repo;
        this.metrics = metrics;
        this.cache = cache;
    }

    @Override
    @Logged
    @Audited("CREATE")
    public Product create(Product p) {
        Product saved = repo.save(p);
        invalidateCache();
        metrics.setProductCount(repo.findAll().size());
        return saved;
    }

    @Override
    @Logged
    public Optional<Product> get(long id) {
        return repo.findById(id);
    }

    @Override
    @Logged
    @Audited("DELETE")
    public boolean delete(long id) {
        boolean ok = repo.deleteById(id);
        if (ok) {
            invalidateCache();
            metrics.setProductCount(repo.findAll().size());
        }
        return ok;
    }

    @Override
    @Logged
    @Audited("UPDATE")
    public Product update(Product p) {
        if (p.getId() == 0 || repo.findById(p.getId()).isEmpty())
            throw new IllegalArgumentException("Product not found");
        Product saved = repo.save(p);
        invalidateCache();
        return saved;
    }

    @Override
    @Logged
    public List<Product> listAll() {
        return repo.findAll();
    }

    @Override
    @Logged
    public List<Product> search(String namePart, String brand, Category category, Double min, Double max, Boolean onlyActive) {
        String key = cacheKey(namePart, brand, category, min, max, onlyActive);
        long t0 = System.currentTimeMillis();
        Optional<List<Long>> cached = cache.getIfPresent(key);
        List<Product> result;

        if (cached.isPresent()) {
            result = idsToProducts(cached.get());

        } else {
            Predicate<Product> pred = p -> true;
            if (namePart != null && !namePart.isBlank()) {
                String q = namePart.toLowerCase();
                pred = pred.and(p -> p.getName().toLowerCase().contains(q) || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q)));
            }
            if (brand != null && !brand.isBlank()) {
                String b = brand.toLowerCase();
                pred = pred.and(p -> p.getBrand() != null && p.getBrand().toLowerCase().contains(b));
            }
            if (category != null) pred = pred.and(p -> p.getCategory() == category);
            if (min != null) pred = pred.and(p -> p.getPrice() >= min);
            if (max != null) pred = pred.and(p -> p.getPrice() <= max);
            if (onlyActive != null && onlyActive) pred = pred.and(Product::isActive);
            result = repo.findAll().stream().filter(pred).collect(Collectors.toList());
            cache.put(key, result.stream().map(Product::getId).collect(Collectors.toList()));
        }
        long dt = System.currentTimeMillis() - t0;
        metrics.setLastQueryMillis(dt);
        metrics.setCache(cache.getHits(), cache.getMisses());
        return result;
    }

    @Override
    @Logged
    public List<Product> paginate(List<Product> list, int page, int size) {
        if (size <= 0) throw new IllegalArgumentException("size must be > 0");
        if (page < 0) throw new IllegalArgumentException("page must be >= 0");
        int from = page * size;
        if (from >= list.size()) return Collections.emptyList();
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
    }

    @Override
    @Logged
    public void persist() throws IOException {
        repo.flush();
    }

    private void invalidateCache() {
        cache.clear();
    }


    private String cacheKey(Object... parts) {
        return Arrays.stream(parts).map(o -> o == null ? "null" : o.toString()).collect(Collectors.joining("|"));
    }

    private List<Product> idsToProducts(List<Long> ids) {
        Map<Long, Product> map = repo.findAll().stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<Product> out = new ArrayList<>();

        for (Long id : ids) {
            Product p = map.get(id);
            if (p != null) out.add(p);
        }

        return out;
    }
}
