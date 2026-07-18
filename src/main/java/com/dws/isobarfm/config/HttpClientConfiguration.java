package com.dws.isobarfm.config;

import com.dws.isobarfm.adapter.out.bandsapi.BandsApiClient;
import com.dws.isobarfm.config.properties.BandsApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties(BandsApiProperties.class)
public class HttpClientConfiguration {

    @Bean
    public BandsApiClient bandsApiClient(BandsApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.connectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.readTimeout().toMillis());

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(org.springframework.web.client.support.RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(BandsApiClient.class);
    }
}
