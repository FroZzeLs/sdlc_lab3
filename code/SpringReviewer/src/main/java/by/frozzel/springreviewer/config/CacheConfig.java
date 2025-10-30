package by.frozzel.springreviewer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CacheConfig {

    @Value("${cache.maxSize:50}")
    private int cacheMaxSize;

    @Bean
    public LruCache<String, Object> lruCache() {
        log.info("Creating LruCache bean with max size: {}", cacheMaxSize);
        return new LruCache<>(cacheMaxSize);
    }
}