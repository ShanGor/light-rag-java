package cn.gzten.rag.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestService {
    private final CacheManager cacheManager;
    @Cacheable(value = "test-cache")
    public String testCacheDetails() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "ok";
    }

    public void doCache() {
        cacheManager.getCache("test-cache").put(SimpleKey.EMPTY, "hello there!");
    }
    public void monitorCache() {
        for (var cacheName : cacheManager.getCacheNames()) {
            log.info("dfault: {}", cacheManager.getCache(cacheName).get(SimpleKey.EMPTY));
        }
    }
}
