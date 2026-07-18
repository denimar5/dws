package com.dws.isobarfm.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(@Valid @NotNull BandsCacheProperties bands) {

    public record BandsCacheProperties(
            @NotNull Duration ttl,
            @Positive int maximumSize
    ) {}
}
