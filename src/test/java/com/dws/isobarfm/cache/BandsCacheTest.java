package com.dws.isobarfm.cache;

import com.dws.isobarfm.adapter.out.bandsapi.BandsApiMapper;
import com.dws.isobarfm.adapter.out.bandsapi.exception.BandsProviderUnavailableException;
import com.dws.isobarfm.application.port.in.FindBandsUseCase;
import com.dws.isobarfm.application.service.BandService;
import com.dws.isobarfm.config.CacheConfiguration;
import com.dws.isobarfm.config.HttpClientConfiguration;
import com.dws.isobarfm.domain.model.Band;
import com.dws.isobarfm.domain.model.BandSort;
import com.dws.isobarfm.domain.model.SortDirection;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        HttpClientConfiguration.class,
        com.dws.isobarfm.adapter.out.bandsapi.BandsApiAdapter.class,
        BandsApiMapper.class,
        BandService.class,
        CacheConfiguration.class
})
class BandsCacheTest {

    static MockWebServer mockWebServer;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        registry.add("clients.bands-api.base-url", () -> mockWebServer.url("/").toString());
        registry.add("clients.bands-api.connect-timeout", () -> "2s");
        registry.add("clients.bands-api.read-timeout", () -> "2s");
        registry.add("app.cache.bands.ttl", () -> "10m");
        registry.add("app.cache.bands.maximum-size", () -> "10");
    }

    @AfterAll
    static void stopMockWebServer() throws IOException {
        mockWebServer.shutdown();
    }

    @Autowired
    private FindBandsUseCase findBandsUseCase;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache("bands").clear();
    }

    private static final String VALID_RESPONSE = """
            [
              {
                "id": "00000000-0000-0000-0000-000000000001",
                "name": "Pink Floyd",
                "image": "https://example.com/pink-floyd.jpg",
                "genre": "progressive rock",
                "biography": "A legendary band.",
                "numPlays": 284212,
                "albums": []
              },
              {
                "id": "00000000-0000-0000-0000-000000000002",
                "name": "Metallica",
                "image": "https://example.com/metallica.jpg",
                "genre": "heavy metal",
                "biography": "Another band.",
                "numPlays": 500000,
                "albums": []
              }
            ]
            """;

    @Test
    void twoCallsUseOneProviderRequest() {
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        int before = mockWebServer.getRequestCount();
        findBandsUseCase.find(null, BandSort.NAME, SortDirection.ASC);
        findBandsUseCase.find(null, BandSort.NAME, SortDirection.ASC);

        assertThat(mockWebServer.getRequestCount() - before).isEqualTo(1);
    }

    @Test
    void differentSearchesUseTheSameCachedList() {
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        int before = mockWebServer.getRequestCount();
        List<Band> allBands = findBandsUseCase.find(null, BandSort.NAME, SortDirection.ASC);
        List<Band> filtered = findBandsUseCase.find("pink", BandSort.NAME, SortDirection.ASC);

        assertThat(mockWebServer.getRequestCount() - before).isEqualTo(1);
        assertThat(allBands).hasSize(2);
        assertThat(filtered).hasSize(1);
    }

    @Test
    void providerExceptionIsNotCached() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        int before = mockWebServer.getRequestCount();
        assertThatThrownBy(() -> findBandsUseCase.find(null, BandSort.NAME, SortDirection.ASC))
                .isInstanceOf(BandsProviderUnavailableException.class);

        List<Band> result = findBandsUseCase.find(null, BandSort.NAME, SortDirection.ASC);
        assertThat(result).hasSize(2);
        assertThat(mockWebServer.getRequestCount() - before).isEqualTo(2);
    }

    @Test
    void cachedListCannotBeExternallyMutated() {
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        List<Band> result = findBandsUseCase.find(null, BandSort.NAME, SortDirection.ASC);

        assertThatThrownBy(() -> result.add(result.get(0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
