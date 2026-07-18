package com.dws.isobarfm.adapter.in.web;

import com.dws.isobarfm.adapter.in.web.mapper.BandWebMapper;
import com.dws.isobarfm.adapter.out.bandsapi.exception.BandsProviderInvalidResponseException;
import com.dws.isobarfm.adapter.out.bandsapi.exception.BandsProviderUnavailableException;
import com.dws.isobarfm.application.port.in.FindBandsUseCase;
import com.dws.isobarfm.application.port.in.GetBandDetailsUseCase;
import com.dws.isobarfm.domain.exception.BandNotFoundException;
import com.dws.isobarfm.domain.model.Band;
import com.dws.isobarfm.domain.model.BandSort;
import com.dws.isobarfm.domain.model.SortDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BandController.class)
@Import({BandWebMapper.class, com.dws.isobarfm.adapter.in.web.error.GlobalExceptionHandler.class})
class BandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FindBandsUseCase findBandsUseCase;

    @MockBean
    private GetBandDetailsUseCase getBandDetailsUseCase;

    private static final UUID BAND_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Band sampleBand() {
        return new Band(BAND_ID, "Pink Floyd", URI.create("https://example.com/pf.jpg"),
                "progressive rock", "Bio here", 284212L, List.of());
    }

    @Test
    void listBandsReturns200() throws Exception {
        when(findBandsUseCase.find(any(), any(), any())).thenReturn(List.of(sampleBand()));

        mockMvc.perform(get("/api/v1/bands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void searchQueryReturnsCorrectCount() throws Exception {
        when(findBandsUseCase.find(anyString(), any(BandSort.class), any(SortDirection.class)))
                .thenReturn(List.of(sampleBand()));

        mockMvc.perform(get("/api/v1/bands?search=pink"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void invalidSortReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/bands?sort=invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
    }

    @Test
    void unknownBandReturns404() throws Exception {
        when(getBandDetailsUseCase.getById(any(UUID.class)))
                .thenThrow(new BandNotFoundException(BAND_ID));

        mockMvc.perform(get("/api/v1/bands/" + BAND_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BAND_NOT_FOUND"));
    }

    @Test
    void providerUnavailableReturns503() throws Exception {
        when(findBandsUseCase.find(any(), any(), any()))
                .thenThrow(new BandsProviderUnavailableException("Unavailable"));

        mockMvc.perform(get("/api/v1/bands"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("BANDS_PROVIDER_UNAVAILABLE"));
    }

    @Test
    void providerInvalidResponseReturns502() throws Exception {
        when(findBandsUseCase.find(any(), any(), any()))
                .thenThrow(new BandsProviderInvalidResponseException("Bad payload"));

        mockMvc.perform(get("/api/v1/bands"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("BANDS_PROVIDER_INVALID_RESPONSE"));
    }

    @Test
    void invalidDirectionReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/bands?sort=name&direction=invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DIRECTION"));
    }

    @Test
    void malformedUuidReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/bands/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void errorResponseContainsRequiredFields() throws Exception {
        when(getBandDetailsUseCase.getById(any(UUID.class)))
                .thenThrow(new BandNotFoundException(BAND_ID));

        mockMvc.perform(get("/api/v1/bands/" + BAND_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.code").value("BAND_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/bands/" + BAND_ID));
    }

    @Test
    void emptySearchReturns200WithEmptyList() throws Exception {
        when(findBandsUseCase.find(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/bands?search=nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.items").isEmpty());
    }
}
