package com.dws.isobarfm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Isobar FM API")
                        .description("""
                                REST API that exposes music band data sourced from the Bands external provider.
                                Supports listing, searching by name, sorting by name or popularity,
                                and retrieving full band details.

                                **Cache**: The full band list is cached in-memory (Caffeine) with a configurable TTL.
                                Filtering and sorting are applied on the cached data — no extra provider calls per query.

                                **Error codes**: All errors return a stable JSON contract with a machine-readable `code` field.
                                """)
                        .version("1.0.0")
                        .license(new License().name("Private")));
    }
}
