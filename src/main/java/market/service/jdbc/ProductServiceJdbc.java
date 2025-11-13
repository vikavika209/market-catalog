package market.service.jdbc;

import market.domain.Category;
import market.domain.Product;
import market.exception.EntityNotFoundException;
import market.exception.PersistenceException;
import market.exception.ValidationException;
import market.repo.ProductRepository;
import market.service.CatalogService;
import market.service.MetricsService;
import market.cache.LRUCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * Сервис каталога товаров, работающий поверх JDBC-репозитория.
 * <p>
 * Отвечает за бизнес-логику:
 * валидацию, поиск/фильтрацию, пагинацию, обновление метрик и работу с кэшем.
 */
public class ProductServiceJdbc implements CatalogService {

    private final ProductRepository repo;
    private final MetricsService metrics;
    private final LRUCache<String, List<Long>> cache;

    /**
     * @param repo    репозиторий товаров (PostgreSQL)
     * @param metrics сервис метрик
     * @param cacheSize размер LRU-кэша для запросов поиска
     */
    public ProductServiceJdbc(ProductRepository repo,
                              MetricsService metrics,
                              int cacheSize) {
        this.repo = repo;
        this.metrics = metrics;
        this.cache = new LRUCache<>(cacheSize);
    }

    @Override
    public Product create(Product p) {
        validateProduct(p);

        long t0 = System.currentTimeMillis();
        Product saved = repo.save(p);
        long dt = System.currentTimeMillis() - t0;

        updateMetrics(dt);
        clearCache();

        return saved;
    }

    @Override
    public Optional<Product> get(long id) {
        long t0 = System.currentTimeMillis();
        Optional<Product> result = repo.findById(id);
        long dt = System.currentTimeMillis() - t0;
        updateMetrics(dt);
        return result;
    }

    @Override
    public Product update(Product p) {
        if (p.getId() == null) {
            throw new ValidationException("ID товара не может быть null при обновлении");
        }
        validateProduct(p);

        if (repo.findById(p.getId()).isEmpty()) {
            throw new EntityNotFoundException("Товар не найден: id=" + p.getId());
        }

        long t0 = System.currentTimeMillis();
        Product saved = repo.save(p);
        long dt = System.currentTimeMillis() - t0;

        updateMetrics(dt);
        clearCache();
        return saved;
    }

    @Override
    public boolean delete(long id) {
        long t0 = System.currentTimeMillis();
        boolean result = repo.deleteById(id);
        long dt = System.currentTimeMillis() - t0;

        updateMetrics(dt);
        if (result) {
            clearCache();
        }
        return result;
    }

    @Override
    public List<Product> listAll() {
        long t0 = System.currentTimeMillis();
        List<Product> list = repo.findAll();
        long dt = System.currentTimeMillis() - t0;
        updateMetrics(dt);
        return list;
    }

    @Override
    public List<Product> search(String q,
                                String brand,
                                Category category,
                                Double minPrice,
                                Double maxPrice,
                                Boolean onlyActive) {

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new ValidationException("Минимальная цена не может быть больше максимальной");
        }

        String key = cacheKey(q, brand, category, minPrice, maxPrice, onlyActive);
        long t0 = System.currentTimeMillis();

        List<Product> result;
        var cached = cache.getIfPresent(key);

        if (cached.isPresent()) {
            result = idsToProducts(cached.get());
        } else {
            Predicate<Product> pred = buildPredicate(q, brand, category, minPrice, maxPrice, onlyActive);
            result = repo.findAll().stream()
                    .filter(pred)
                    .collect(Collectors.toList());
            cache.put(key, result.stream().map(Product::getId).collect(Collectors.toList()));
        }

        long dt = System.currentTimeMillis() - t0;
        metrics.setLastQueryMillis(dt);
        metrics.setCache(cache.getHits(), cache.getMisses());
        metrics.setProductCount(repo.findAll().size());

        return result;
    }

    @Override
    public List<Product> paginate(List<Product> list, int page, int size) {
        if (size <= 0) {
            throw new ValidationException("Размер страницы должен быть > 0");
        }
        if (page < 0) {
            throw new ValidationException("Номер страницы не может быть отрицательным");
        }

        int from = page * size;
        if (from >= list.size()) {
            return List.of();
        }
        int to = Math.min(from + size, list.size());
        return new ArrayList<>(list.subList(from, to));
    }

    @Override
    public void persist() {
        try {
            repo.flush();
        } catch (IOException e) {
            throw new PersistenceException("Ошибка сохранения данных в репозитории: " + e.getMessage());
        }
    }

    private void validateProduct(Product p) {
        if (p.getName() == null || p.getName().isBlank()) {
            throw new ValidationException("Название товара обязательно");
        }
        if (p.getCategory() == null) {
            throw new ValidationException("Категория товара обязательна");
        }
        if (p.getPrice() < 0) {
            throw new ValidationException("Цена не может быть отрицательной");
        }
    }

    private void updateMetrics(long lastQueryMs) {
        metrics.setLastQueryMillis(lastQueryMs);
        metrics.setProductCount(repo.findAll().size());
    }

    private void clearCache() {
        cache.clear();
        metrics.setCache(cache.getHits(), cache.getMisses());
    }

    private Predicate<Product> buildPredicate(String q,
                                              String brand,
                                              Category category,
                                              Double min,
                                              Double max,
                                              Boolean onlyActive) {

        Predicate<Product> pred = p -> true;

        if (q != null && !q.isBlank()) {
            String text = q.toLowerCase(Locale.ROOT);
            pred = pred.and(p ->
                    (p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(text)) ||
                            (p.getDescription() != null && p.getDescription().toLowerCase(Locale.ROOT).contains(text))
            );
        }

        if (brand != null && !brand.isBlank()) {
            String b = brand.toLowerCase(Locale.ROOT);
            pred = pred.and(p ->
                    p.getBrand() != null &&
                            p.getBrand().toLowerCase(Locale.ROOT).contains(b)
            );
        }

        if (category != null) {
            pred = pred.and(p -> p.getCategory() == category);
        }

        if (min != null) {
            pred = pred.and(p -> p.getPrice() >= min);
        }
        if (max != null) {
            pred = pred.and(p -> p.getPrice() <= max);
        }

        if (onlyActive != null && onlyActive) {
            pred = pred.and(Product::isActive);
        }

        return pred;
    }

    private String cacheKey(String q,
                            String brand,
                            Category category,
                            Double min,
                            Double max,
                            Boolean onlyActive) {
        return (q == null ? "_" : q.trim()) + "|" +
                (brand == null ? "_" : brand.trim()) + "|" +
                (category == null ? "_" : category.name()) + "|" +
                (min == null ? "_" : min) + "|" +
                (max == null ? "_" : max) + "|" +
                (onlyActive == null ? "_" : onlyActive);
    }

    private List<Product> idsToProducts(List<Long> ids) {
        List<Product> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            repo.findById(id).ifPresent(result::add);
        }
        return result;
    }
}
