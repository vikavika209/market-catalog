package market.service;

public class MetricsServiceImpl implements  MetricsService{
    private volatile long lastQueryMillis;
    private volatile int productCount;
    private volatile long cacheHits;
    private volatile long cacheMisses;

    @Override
    public void setLastQueryMillis(long ms){
        this.lastQueryMillis = ms;
    }

    @Override
    public void setProductCount(int n){
        this.productCount = n;
    }

    @Override
    public void setCache(long hits, long misses){
        this.cacheHits = hits; this.cacheMisses = misses;
    }

    @Override
    public String snapshot(){
        return "--- Metrics ---\n" +
               "products: " + productCount + "\n" +
               "lastQueryMs: " + lastQueryMillis + "\n" +
               "cache: hits=" + cacheHits + ", misses=" + cacheMisses + "\n";
    }
}
