package com.dws.isobarfm.adapter.out.bandsapi;

import com.dws.isobarfm.adapter.out.bandsapi.exception.BandsProviderInvalidResponseException;
import com.dws.isobarfm.adapter.out.bandsapi.exception.BandsProviderUnavailableException;
import com.dws.isobarfm.application.port.out.LoadBandsPort;
import com.dws.isobarfm.domain.model.Band;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class BandsApiAdapter implements LoadBandsPort {

    private static final Logger log = LoggerFactory.getLogger(BandsApiAdapter.class);

    private final BandsApiClient bandsApiClient;
    private final BandsApiMapper mapper;

    public BandsApiAdapter(BandsApiClient bandsApiClient, BandsApiMapper mapper) {
        this.bandsApiClient = bandsApiClient;
        this.mapper = mapper;
    }

    @Override
    @Cacheable(cacheNames = "bands", key = "'all'", sync = true)
    public List<Band> loadAll() {
        long start = System.currentTimeMillis();
        try {
            List<BandsApiResponse> responses = bandsApiClient.getBands();
            log.debug("Bands API responded in {} ms", System.currentTimeMillis() - start);

            if (responses == null) {
                throw new BandsProviderInvalidResponseException("Provider returned null response");
            }

            return responses.stream().map(mapper::toDomain).toList();

        } catch (BandsProviderInvalidResponseException | BandsProviderUnavailableException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.warn("Bands provider is unavailable: {}", e.getMessage());
            throw new BandsProviderUnavailableException("Bands provider is unavailable", e);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status >= 500) {
                log.warn("Bands provider returned server error: HTTP {}", status);
                throw new BandsProviderUnavailableException(
                        "Bands provider returned server error: HTTP " + status, e);
            }
            log.warn("Bands provider returned client error: HTTP {}", status);
            throw new BandsProviderInvalidResponseException(
                    "Bands provider returned unexpected client error: HTTP " + status, e);
        } catch (Exception e) {
            if (isPayloadError(e)) {
                log.warn("Bands provider returned invalid or incompatible payload: {}", e.getMessage());
                throw new BandsProviderInvalidResponseException(
                        "Bands provider returned invalid or incompatible payload", e);
            }
            log.error("Unexpected error calling bands provider", e);
            throw new BandsProviderUnavailableException("Unexpected error from bands provider", e);
        }
    }

    private boolean isPayloadError(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            String name = cause.getClass().getName();
            if (name.contains("HttpMessageConversion")
                    || name.contains("HttpMessageNotReadable")
                    || name.contains("JsonProcessingException")
                    || name.contains("JsonParseException")
                    || name.contains("MismatchedInputException")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
