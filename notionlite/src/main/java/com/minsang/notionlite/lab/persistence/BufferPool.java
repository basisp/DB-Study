package com.minsang.notionlite.lab.persistence;

// 최근 사용 순서를 유지하는 맵이라 LRU 캐시 구현에 적합합니다.
import java.util.LinkedHashMap;
import java.util.Map;
// 캐시 미스 시 값을 어떻게 읽어올지 함수로 전달받습니다.
import java.util.function.Function;

/**
 * Minimal LRU buffer-pool mimic.
 *
 * It demonstrates cache hit/miss behavior for pages.
 */
public class BufferPool<K, V> {
    // 버퍼 풀에 최대 몇 개의 페이지를 둘지 정합니다.
    private final int capacity;
    // 실제 캐시 저장소입니다.
    private final Map<K, V> cache;

    public BufferPool(int capacity) {
        this.capacity = capacity;
        // accessOrder=true 이므로 최근 접근한 항목이 뒤로 이동합니다.
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                // 용량을 초과하면 가장 오래 안 쓰인 페이지를 내보냅니다.
                return size() > BufferPool.this.capacity;
            }
        };
    }

    public synchronized V getOrLoad(K key, Function<K, V> loader) {
        // 먼저 메모리 캐시에 있는지 확인합니다.
        V cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        // 없으면 디스크 등 외부 저장소에서 읽어옵니다.
        V loaded = loader.apply(key);
        // 읽어온 뒤 캐시에 넣어서 다음 접근을 빠르게 만듭니다.
        cache.put(key, loaded);
        return loaded;
    }

    public synchronized int size() {
        // 현재 메모리에 유지 중인 페이지 수입니다.
        return cache.size();
    }
}
