package com.dws.isobarfm.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "clients.bands-api")
public record BandsApiProperties(
        @NotNull URI baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {}
