package com.dws.isobarfm.adapter.out.bandsapi;

import com.dws.isobarfm.adapter.out.bandsapi.exception.BandsProviderInvalidResponseException;
import com.dws.isobarfm.adapter.out.bandsapi.exception.BandsProviderUnavailableException;
import com.dws.isobarfm.config.HttpClientConfiguration;
import com.dws.isobarfm.domain.model.Band;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {HttpClientConfiguration.class, BandsApiAdapter.class, BandsApiMapper.class})
class BandsApiAdapterTest {

    static MockWebServer mockWebServer;

    @BeforeAll
    static void startMockWebServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void stopMockWebServer() throws IOException {
        mockWebServer.shutdown();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        registry.add("clients.bands-api.base-url", () -> mockWebServer.url("/").toString());
        registry.add("clients.bands-api.connect-timeout", () -> "2s");
        registry.add("clients.bands-api.read-timeout", () -> "2s");
    }

    @Autowired
    private BandsApiAdapter bandsApiAdapter;

    private static final String VALID_RESPONSE = """
            [
              {
                "id": "00000000-0000-0000-0000-000000000001",
                "name": "Pink Floyd",
                "image": "https://example.com/pink-floyd.jpg",
                "genre": "progressive rock",
                "biography": "A legendary band.",
                "numPlays": 284212,
                "albums": ["00000000-0000-0000-0000-000000000010"]
              }
            ]
            """;

    @Test
    void deserializeValidProviderResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        List<Band> bands = bandsApiAdapter.loadAll();

        assertThat(bands).hasSize(1);
    }

    @Test
    void mapProviderDtoToDomain() {
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        List<Band> bands = bandsApiAdapter.loadAll();
        Band band = bands.get(0);

        assertThat(band.name()).isEqualTo("Pink Floyd");
        assertThat(band.genre()).isEqualTo("progressive rock");
        assertThat(band.numPlays()).isEqualTo(284212L);
        assertThat(band.albumIds()).hasSize(1);
    }

    @Test
    void provider500BecomesProviderUnavailableException() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> bandsApiAdapter.loadAll())
                .isInstanceOf(BandsProviderUnavailableException.class);
    }

    @Test
    void invalidPayloadBecomesInvalidResponseException() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("not-json-at-all{{{")
                .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> bandsApiAdapter.loadAll())
                .isInstanceOf(BandsProviderInvalidResponseException.class);
    }

    @Test
    void baseUrlIsConfigurable() {
        mockWebServer.enqueue(new MockResponse()
                .setBody(VALID_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        List<Band> bands = bandsApiAdapter.loadAll();

        assertThat(bands).isNotNull();
    }
}
