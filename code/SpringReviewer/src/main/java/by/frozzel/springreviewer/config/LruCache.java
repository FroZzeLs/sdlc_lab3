package by.frozzel.springreviewer.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LruCache<K, V> {

    private final Map<K, V> cache;
    private final int maxSize;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static final String LOG_CACHE_HIT = "LRU Cache HIT for key: {}";
    private static final String LOG_CACHE_MISS = "LRU Cache MISS for key: {}";
    private static final String LOG_CACHE_PUT = "Putting data into LRU cache with key: {}";
    private static final String LOG_CACHE_REMOVE = "Removing data from LRU cache with key: {}";

    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be positive");
        }
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                boolean shouldRemove = size() > LruCache.this.maxSize;
                if (shouldRemove) {
                    log.info("LRU Cache limit ({}) reached. Removing eldest "
                                   + "(least recently used) entry with key: {}",
                            LruCache.this.maxSize, eldest.getKey());
                }
                return shouldRemove;
            }
        };
        log.info("LruCache instance created with max size: {}", this.maxSize);
    }

    public V get(K key) {
        lock.readLock().lock();
        try {
            V value = cache.get(key);
            if (value != null) {
                log.info(LOG_CACHE_HIT, key);
            } else {
                log.info(LOG_CACHE_MISS, key);
            }
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            log.info(LOG_CACHE_PUT, key);
            cache.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void remove(K key) {
        lock.writeLock().lock();
        try {
            log.info(LOG_CACHE_REMOVE, key);
            cache.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }
}