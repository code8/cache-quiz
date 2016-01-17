/**
 * Created by code8 on 1/15/16.
 */

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;


public class JDKCache<K,V> extends LinkedHashMap<K,V> implements Cache<K,V> {
    private static final float LOAD_FACTOR = 0.75f;
    protected final int capacity;
    protected final Function<K,V> dataSource;

    public JDKCache(Function<K,V> dataSource, int capacity, boolean lru) {
        super(capacity, LOAD_FACTOR, lru);
        this.capacity = capacity;
        this.dataSource = dataSource;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

    @Override
    public V read(K key) {
        return computeIfAbsent(key, dataSource);
    }

    @Override
    public int usage() {
        return size();
    }
}
