package me.pcbeast.multilevelcache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class MultilevelCacheTest {

    private MultilevelCache multilevelCache;

    @Mock
    private CacheManager firstLevelCacheManager;
    @Mock
    private CacheManager secondLevelCacheManager;
    @Mock
    private Cache firstLevelCache;
    @Mock
    private Cache secondLevelCache;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(firstLevelCacheManager.getCache("testCache")).thenReturn(firstLevelCache);
        when(secondLevelCacheManager.getCache("testCache")).thenReturn(secondLevelCache);

        multilevelCache = new MultilevelCache("testCache", Arrays.asList(firstLevelCacheManager, secondLevelCacheManager));
    }

    @Test
    public void testGetWithCacheHitOnFirstLevel() {
        // Setup: Cache hit on the first level
        when(firstLevelCache.get("key")).thenReturn(new SimpleValueWrapper("value"));

        // Execution
        Object value = multilevelCache.get("key").get();

        // Assertion
        assertEquals("value", value);
        verify(firstLevelCache, times(1)).get("key");
        verify(secondLevelCache, never()).get("key");

        // No repopulation should occur as the value was found in the first level
        verify(firstLevelCache, never()).put(eq("key"), any());
        verify(secondLevelCache, never()).put(eq("key"), any());
    }

    @Test
    public void testGetWithCacheHitOnSecondLevel() {
        // Setup: Cache miss on the first level, hit on the second
        when(firstLevelCache.get("key")).thenReturn(null);
        when(secondLevelCache.get("key")).thenReturn(() -> "value");

        // Execution
        Cache.ValueWrapper valueWrapper = multilevelCache.get("key");

        // Assertion
        assertEquals("value", valueWrapper.get()); // Unwrap the value
        verify(firstLevelCache, times(1)).get("key");
        verify(secondLevelCache, times(1)).get("key");

        // Check repopulation of higher level cache with the ValueWrapper
        verify(firstLevelCache, times(1)).put(eq("key"), eq(valueWrapper));
    }

    @Test
    public void testGetWithCacheMissOnAllLevels() {
        // Setup: Cache miss on all levels
        when(firstLevelCache.get("key")).thenReturn(null);
        when(secondLevelCache.get("key")).thenReturn(null);

        // Execution
        Cache.ValueWrapper valueWrapper = multilevelCache.get("key");

        // Assertion
        assertNull(valueWrapper);
        verify(firstLevelCache, times(1)).get("key");
        verify(secondLevelCache, times(1)).get("key");
    }

    @Test
    public void testPut() {
        // Execution
        multilevelCache.put("key", "value");

        // Assertion
        verify(firstLevelCache, times(1)).put("key", "value");
        verify(secondLevelCache, times(1)).put("key", "value");
    }

    @Test
    public void testEvict() {
        // Execution
        multilevelCache.evict("key");

        // Assertion
        verify(firstLevelCache, times(1)).evict("key");
        verify(secondLevelCache, times(1)).evict("key");
    }

    @Test
    public void testClear() {
        // Execution
        multilevelCache.clear();

        // Assertion
        verify(firstLevelCache, times(1)).clear();
        verify(secondLevelCache, times(1)).clear();
    }
}
