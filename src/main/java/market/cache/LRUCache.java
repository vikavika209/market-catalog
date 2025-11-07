
package market.cache;
import java.util.*;
public class LRUCache<K,V> extends LinkedHashMap<K,V> {
    private final int capacity;
    private long hits, misses;
    public LRUCache(int capacity){
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }
    @Override protected boolean removeEldestEntry(Map.Entry<K,V> eldest){
        return size() > capacity;
    }
    public Optional<V> getIfPresent(K key){
        if (super.containsKey(key)) { hits++; return Optional.of(super.get(key)); }
        misses++; return Optional.empty();
    }
    public long getHits(){ return hits; }
    public long getMisses(){ return misses; }
}
