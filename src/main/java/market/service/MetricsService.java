
package market.service;
public class MetricsService {
    private volatile long lastQueryMillis;
    private volatile int productCount;
    private volatile long cacheHits;
    private volatile long cacheMisses;
    public void setLastQueryMillis(long ms){ this.lastQueryMillis = ms; }
    public void setProductCount(int n){ this.productCount = n; }
    public void setCache(long hits, long misses){ this.cacheHits = hits; this.cacheMisses = misses; }
    public String snapshot(){
        return "--- Metrics ---\n" +
               "products: " + productCount + "\n" +
               "lastQueryMs: " + lastQueryMillis + "\n" +
               "cache: hits=" + cacheHits + ", misses=" + cacheMisses + "\n";
    }
}
