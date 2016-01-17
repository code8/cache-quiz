import java.util.function.Function;

/**
 * Created by code8 on 1/15/16.
 */

public interface Cache<K,V> {
    V read(K key);

    // capacity adjusted to the nearest power of two
    static <K,V> Cache<K,V> make(String type, Function<K,V> dataSource, int capacity, boolean lru) {
        switch (type.toLowerCase()) {
            case "jdk":
                return new JDKCache<>(dataSource, capacity, lru);

            case "custom":
                return new CustomCache<>(dataSource, capacity, lru);

            default:
                throw new RuntimeException("Can`t determine type of Cache, try 'jdk' or 'custom'");

        }
    }

    // test purpose only
    V get(K key);
    int usage();
}
