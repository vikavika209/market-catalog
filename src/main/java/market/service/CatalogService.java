
package market.service;
import market.cache.LRUCache;
import market.domain.*;
import market.repo.ProductRepository;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
public class CatalogService {
    private final ProductRepository repo;
    private final LRUCache<String, List<Long>> cache;
    private final MetricsService metrics;
    public CatalogService(ProductRepository repo, MetricsService metrics){
        this.repo = repo;
        this.metrics = metrics;
        this.cache = new LRUCache<>(64);
        repo.load();
        metrics.setProductCount(repo.findAll().size());
    }
    public Product create(Product p){
        Product saved = repo.save(p);
        invalidateCache();
        metrics.setProductCount(repo.findAll().size());
        return saved;
    }
    public Optional<Product> get(long id){ return repo.findById(id); }
    public boolean delete(long id){
        boolean ok = repo.deleteById(id);
        if (ok){ invalidateCache(); metrics.setProductCount(repo.findAll().size()); }
        return ok;
    }
    public Product update(Product p){
        if (p.getId()==0 || repo.findById(p.getId()).isEmpty())
            throw new IllegalArgumentException("Product not found");
        Product saved = repo.save(p);
        invalidateCache();
        return saved;
    }
    public List<Product> listAll(){ return repo.findAll(); }
    public List<Product> search(String namePart, String brand, Category category, Double min, Double max, Boolean onlyActive){
        String key = cacheKey(namePart, brand, category, min, max, onlyActive);
        long t0 = System.currentTimeMillis();
        Optional<List<Long>> cached = cache.getIfPresent(key);
        List<Product> result;
        if (cached.isPresent()){
            result = idsToProducts(cached.get());
        } else {
            Predicate<Product> pred = p -> true;
            if (namePart != null && !namePart.isBlank()){
                String q = namePart.toLowerCase();
                pred = pred.and(p -> p.getName().toLowerCase().contains(q) ||
                                   (p.getDescription()!=null && p.getDescription().toLowerCase().contains(q)));
            }
            if (brand != null && !brand.isBlank()){
                String b = brand.toLowerCase();
                pred = pred.and(p -> p.getBrand()!=null && p.getBrand().toLowerCase().contains(b));
            }
            if (category != null) pred = pred.and(p -> p.getCategory()==category);
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
    public List<Product> paginate(List<Product> list, int page, int size){
        if (size<=0) throw new IllegalArgumentException("size must be > 0");
        if (page<0) throw new IllegalArgumentException("page must be >= 0");
        int from = page * size;
        if (from >= list.size()) return Collections.emptyList();
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
    }
    public void persist(){ repo.flush(); }
    private void invalidateCache(){ cache.clear(); }
    private String cacheKey(Object... parts){
        return Arrays.stream(parts).map(o -> o==null? "null" : o.toString()).collect(Collectors.joining("|"));
    }
    private List<Product> idsToProducts(List<Long> ids){
        Map<Long, Product> map = repo.findAll().stream().collect(Collectors.toMap(Product::getId, p->p));
        List<Product> out = new ArrayList<>();
        for (Long id: ids){ Product p = map.get(id); if (p!=null) out.add(p); }
        return out;
    }
}
