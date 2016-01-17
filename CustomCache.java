import java.util.Objects;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

/**
 * Created by code8 on 1/15/16.
 */

public class CustomCache<K,V> implements Cache<K,V> {
    protected final int capacity;
    protected final int indexMask;
    protected final Function<K,V> dataSource;
    protected final boolean lru;
    protected final StampedLock lock = new StampedLock();
    protected final Entry<K,V>[] cache;
    protected final Entry<K,V> head;
    protected volatile int used;

    protected static class Entry<K,V> {
        final K key;
        final V value;
        Entry<K,V> nextInList;
        Entry<K,V> prevInList;
        Entry<K,V> nextInBucket;
        Entry<K,V> prevInBucket;

        protected Entry(K key, V value, Entry<K,V> nextInBucket) {
            nextInList = this;
            prevInList = this;
            this.nextInBucket = nextInBucket;
            if (nextInBucket != null) {
                nextInBucket.prevInBucket = this;
            }
            this.key = key;
            this.value = value;
        }

        void collapseList() {
            if (prevInList != null) {
                prevInList.nextInList = nextInList;
            }
            if (nextInList != null) {
                nextInList.prevInList = prevInList;
            }
        }

        void collapseBucket() {
            if (prevInBucket != null) {
                prevInBucket.nextInBucket = nextInBucket;
            }
            if (nextInBucket != null) {
                nextInBucket.prevInBucket = prevInBucket;
            }
        }

        void afterList(Entry<K,V> prev) {
            if (prev.nextInList != null) {
                prev.nextInList.prevInList = this;
            }
            nextInList = prev.nextInList;
            prevInList = prev;
            prev.nextInList = this;
        }

    }

    protected static int toNearPowerOfTwo(int value) {
        if (value < 0) {
            throw new RuntimeException("negative capacity");
        }

        if (value >= Integer.MIN_VALUE >>> 1) {
            return Integer.MAX_VALUE;
        }

        int v = Integer.MIN_VALUE >>> 2;
        while ((v & value) == 0) v >>= 1;

        return v == value ? v : v << 1;
    }

    protected int makeIndex(int hash) {
        return hash & indexMask;
    }

    @SuppressWarnings("unchecked")
    public CustomCache(Function<K,V> dataSource, int capacity, boolean lru) {
        head = new Entry<>(null, null, null);
        this.capacity = capacity;
        // capacity adjusted to the nearest power of two
        int cacheSize = toNearPowerOfTwo(capacity);
        indexMask = cacheSize - 1;
        this.cache = new Entry[cacheSize];
        this.dataSource = dataSource;
        this.lru = lru;
    }

    @Override
    public V read(K key) {
        long stamp;
        Entry<K,V> entry = null;

        if (!lru) {
            stamp = lock.tryOptimisticRead();

            entry = tryExisted(key);

            if (lock.validate(stamp) && entry != null) {
                // value - final field
                return entry.value;
            }
        }

        stamp = lock.readLock();
        try {
            entry = tryExisted(key);
        } finally {
            if (entry != null && !lru) {
                lock.unlockRead(stamp);
                return entry.value;
            }
        }

        long writeStamp;
        if ((writeStamp = lock.tryConvertToWriteLock(stamp)) == 0) {
            lock.unlockRead(stamp);
            writeStamp = lock.writeLock();
        }

        try {
            entry = tryExisted(key);

            if (entry != null && lru) {
                updateAccess(entry);

            } else if(entry == null) {
                entry = makeNewEntry(key);

            }
            return entry.value;

        } finally {
            lock.unlockWrite(writeStamp);
        }

    }

    @Override
    // not thread safe use this method to test purpose only
    public V get(K key) {
        Entry<K,V> entry = cache[makeIndex(key == null ? 0 : key.hashCode())];
        return entry == null ? null : entry.value;
    }

    @Override
    // use it to test purpose only
    public int usage() {
        return used;
    }

    protected Entry<K,V> tryExisted(K key) {
        Entry<K,V> entry = cache[makeIndex(key == null ? 0 : key.hashCode())];

        while (entry != null && !Objects.equals(entry.key, key)) {
            entry = entry.nextInBucket;
        }

        return entry;
    }

    protected void updateAccess(Entry<K,V> entry) {
        entry.collapseList();
        entry.afterList(head);
    }

    protected Entry<K,V> makeNewEntry(K key) {
        int used = this.used, hashCode;
        Entry<K,V> firstInBucket;

        if (used >= capacity) {
            Entry<K,V> staleEntry = head.prevInList;
            staleEntry.collapseList();
            hashCode = staleEntry.key == null ? 0 : staleEntry.key.hashCode();
            firstInBucket = cache[makeIndex(hashCode)];
            if (firstInBucket == staleEntry) {
                cache[makeIndex(hashCode)] = staleEntry.nextInBucket;
            }
            staleEntry.collapseBucket();

        } else {
            used++;
            this.used = used;
        }

        firstInBucket = cache[makeIndex(key == null ? 0 : key.hashCode())];
        Entry<K,V> entry = new Entry<>(key, dataSource.apply(key), firstInBucket);
        entry.afterList(head);
        hashCode = key == null ? 0 : key.hashCode();
        cache[makeIndex(hashCode)] = entry;
        return entry;
    }
}

