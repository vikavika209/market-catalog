package market.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Реализация кэша с политикой вытеснения LRU (Least Recently Used).
 * <p>
 * Хранит ограниченное количество элементов и автоматически удаляет
 * наименее недавно использованные записи при превышении заданной ёмкости.
 * <p>
 * Кроме того, собирает статистику обращений — количество попаданий и промахов.
 *
 * @param <K> тип ключа
 * @param <V> тип значения
 */
public class LRUCache<K,V> extends LinkedHashMap<K,V> {
    /** Максимальное количество элементов в кэше. */
    private final int capacity;

    /** Количество успешных обращений к кэшу (cache hits). */
    private long hits;

    /** Количество промахов кэша (cache misses). */
    private long misses;

    /**
     * Создаёт новый LRU-кэш с заданной ёмкостью.
     *
     * @param capacity максимальное количество элементов, которые может хранить кэш
     */
    public LRUCache(int capacity) {
        // accessOrder = true — порядок записей зависит от порядка доступа
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    /**
     * Удаляет самый старый элемент при превышении ёмкости.
     *
     * @param eldest самая старая запись
     * @return {@code true}, если элемент нужно удалить
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

    /**
     * Возвращает значение, если ключ присутствует в кэше.
     * <p>
     * Метод также обновляет статистику — увеличивает количество {@code hits} или {@code misses}.
     *
     * @param key ключ для поиска
     * @return {@link Optional} с найденным значением, если элемент присутствует;
     *         {@link Optional#empty()} — если элемент отсутствует в кэше
     */
    public Optional<V> getIfPresent(K key) {
        if (super.containsKey(key)) {
            hits++;
            return Optional.of(super.get(key));
        }
        misses++;
        return Optional.empty();
    }

    /**
     * Возвращает количество успешных обращений к кэшу (cache hits).
     *
     * @return количество попаданий
     */
    public long getHits() {
        return hits;
    }

    /**
     * Возвращает количество промахов кэша (cache misses).
     *
     * @return количество промахов
     */
    public long getMisses() {
        return misses;
    }
}
