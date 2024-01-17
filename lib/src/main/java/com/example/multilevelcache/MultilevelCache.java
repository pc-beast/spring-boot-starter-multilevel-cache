package com.example.multilevelcache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.concurrent.Callable;

public class MultilevelCache implements Cache {

    private final String name;
    private final List<CacheManager> cacheManagers;

    public MultilevelCache(String name, List<CacheManager> cacheManagers) {
        this.name = name;
        this.cacheManagers = cacheManagers;
    }

    /**
     * Return the cache name.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Return the underlying native cache provider.
     * For simplicity, return first non-null cache's native cache.
     *
     * @return underlying native cache provider
     */
    @Override
    public Object getNativeCache() {
        // return first non-null cache's native cache
        for (CacheManager cacheManager : cacheManagers) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                return cache.getNativeCache();
            }
        }
        return null;
    }

    @Override
    public ValueWrapper get(Object key) {
        // Loop through all caches and return the first non-null value
        for (CacheManager cacheManager : cacheManagers) {
            Cache cache = cacheManager.getCache(name);

            // Null check for cache
            if (cache != null) {
                ValueWrapper value = cache.get(key);
                // If value is not null, repopulate higher level caches
                // and return the value
                if (value != null) {
                    // Repopulate higher level caches
                    repopulateHigherLevelCaches(key, value, cache);
                    return value;
                }
            }
        }
        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        for (CacheManager manager : cacheManagers) {
            Cache cache = manager.getCache(name);

            // Null check for cache
            if (cache != null) {
                T value = cache.get(key, type);
                if (value != null) {
                    // Repopulate caches in higher levels
                    repopulateHigherLevelCaches(key, value, cache);
                    return value;
                }
            }
        }
        return null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        for (CacheManager manager : cacheManagers) {
            Cache cache = manager.getCache(name);
            // Null check for cache
            if (cache != null) {
                T value = cache.get(key, valueLoader);
                if (value != null) {
                    // Repopulate caches in higher levels
                    repopulateHigherLevelCaches(key, value, cache);
                    return value;
                }
            }
        }
        return null;
    }

    @Override
    public void put(Object key, Object value) {
        for (CacheManager manager : cacheManagers) {
            Cache cache = manager.getCache(name);
            if (cache != null) {
                cache.put(key, value);
            }
        }
    }

    @Override
    public void evict(Object key) {
        for (CacheManager manager : cacheManagers) {
            Cache cache = manager.getCache(name);
            if (cache != null) {
                cache.evict(key);
            }
        }
    }

    @Override
    public void clear() {
        for (CacheManager manager : cacheManagers) {
            Cache cache = manager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    /**
     * Repopulate higher level caches with the given key and value.
     *
     * @param key         the key whose associated value is to be returned
     * @param value       the value to be associated with the specified key
     * @param sourceCache the cache from which the key and value are obtained
     */
    private void repopulateHigherLevelCaches(Object key, Object value, Cache sourceCache) {
        for (CacheManager higherLevelCacheManager : cacheManagers) {
            Cache higherLevelCache = higherLevelCacheManager.getCache(name);

            // If higher level cache is the same as source cache, break
            if (higherLevelCache == sourceCache) {
                break;
            }

            // Repopulate higher level cache
            if (higherLevelCache != null) {
                higherLevelCache.put(key, value);
            }
        }
    }
}
