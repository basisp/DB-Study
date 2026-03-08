package com.minsang.notionlite.lab.persistence;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Minimal LRU buffer-pool mimic.
 *
 * It demonstrates cache hit/miss behavior for pages.
 */
public class BufferPool<K, V> {
    private final int capacity;
    private final Map<K, V> cache;

    public BufferPool(int capacity) {
        this.capacity = capacity;
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > BufferPool.this.capacity;
            }
        };
    }

    public synchronized V getOrLoad(K key, Function<K, V> loader) {
        V cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        V loaded = loader.apply(key);
        cache.put(key, loaded);
        return loaded;
    }

    public synchronized int size() {
        return cache.size();
    }
}
