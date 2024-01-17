package com.example.multilevelcache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.AbstractCacheManager;

import java.util.*;
import java.util.stream.Collectors;

public class MultilevelCacheManager extends AbstractCacheManager {
    private final List<CacheManager> cacheManagers = new ArrayList<>();

    public MultilevelCacheManager(CacheManager... cacheManagers) {
        this.cacheManagers.addAll(Arrays.asList(cacheManagers));
    }

    /**
     * Return a collection of the caches known by this cache manager.
     *
     * @return list of caches
     */
    @Override
    protected Collection<? extends Cache> loadCaches() {
        List<Cache> caches = new ArrayList<>();
        for (CacheManager cacheManager : cacheManagers) {
            // Get all not null caches
            caches.addAll(cacheManager.getCacheNames().stream()
                    .map(cacheManager::getCache)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(ArrayList::new)));
        }
        return caches;
    }

    @Override
    public Cache getCache(String name) {
        for (CacheManager cacheManager : cacheManagers) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                return new MultilevelCache(name, cacheManagers);
            }
        }
        return null;
    }
}
