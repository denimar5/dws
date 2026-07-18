package com.dws.isobarfm.config;

import com.dws.isobarfm.config.properties.CacheProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager(CacheProperties cacheProperties) {
        CaffeineCacheManager manager = new CaffeineCacheManager("bands");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(cacheProperties.bands().ttl())
                .maximumSize(cacheProperties.bands().maximumSize()));
        return manager;
    }
}
