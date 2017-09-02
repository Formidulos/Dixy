package com.avairebot.orion.cache.adapters;

import com.avairebot.orion.cache.CacheItem;
import com.avairebot.orion.contracts.cache.CacheAdapter;
import com.avairebot.orion.contracts.cache.CacheClosure;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class MemoryAdapter extends CacheAdapter {

    private final Map<String, CacheItem> caches = Collections.synchronizedMap(new WeakHashMap<String, CacheItem>());

    @Override
    public synchronized boolean put(String token, Object value, int seconds) {
        caches.put(token, new CacheItem(token, value, System.currentTimeMillis() + (seconds * 1000)));
        return true;
    }

    @Override
    public synchronized Object remember(String token, int seconds, CacheClosure closure) {
        if (has(token)) {
            return get(token);
        }

        CacheItem item = new CacheItem(token, closure.run(), System.currentTimeMillis() + (seconds * 1000));
        caches.put(token, item);

        return item.getValue();
    }

    @Override
    public synchronized boolean forever(String token, Object value) {
        caches.put(token, new CacheItem(token, value, -1));

        return true;
    }

    @Override
    public synchronized Object get(String token) {
        if (!has(token)) {
            return null;
        }

        CacheItem item = getRaw(token);
        if (item == null) {
            return null;
        }
        return item.getValue();
    }

    @Override
    public synchronized CacheItem getRaw(String token) {
        return caches.getOrDefault(token, null);
    }

    @Override
    public synchronized boolean has(String token) {
        return caches.containsKey(token) && getRaw(token).isExpired();
    }

    @Override
    public synchronized CacheItem forget(String token) {
        return caches.remove(token);
    }

    @Override
    public synchronized boolean flush() {
        caches.clear();
        return true;
    }
}
