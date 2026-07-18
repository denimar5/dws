package com.dws.isobarfm.application.service;

import com.dws.isobarfm.application.port.out.LoadBandsPort;
import com.dws.isobarfm.domain.exception.BandNotFoundException;
import com.dws.isobarfm.domain.model.Band;
import com.dws.isobarfm.domain.model.BandSort;
import com.dws.isobarfm.domain.model.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BandServiceTest {

    @Mock
    private LoadBandsPort loadBandsPort;

    private BandService bandService;

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ID_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private Band metallica;
    private Band pinkFloyd;
    private Band acDc;

    @BeforeEach
    void setUp() {
        bandService = new BandService(loadBandsPort);

        metallica = new Band(ID_1, "Metallica", URI.create("https://example.com/metallica.jpg"),
                "heavy metal", "Bio of Metallica", 500_000L, List.of());
        pinkFloyd = new Band(ID_2, "Pink Floyd", URI.create("https://example.com/pinkfloyd.jpg"),
                "progressive rock", "Bio of Pink Floyd", 284_212L, List.of());
        acDc = new Band(ID_3, "AC/DC", URI.create("https://example.com/acdc.jpg"),
                "hard rock", "Bio of AC/DC", 1_000_000L, List.of());
    }

    @Test
    void returnAllBands() {
        when(loadBandsPort.loadAll()).thenReturn(List.of(metallica, pinkFloyd, acDc));

        List<Band> result = bandService.find(null, BandSort.NAME, SortDirection.ASC);

        assertThat(result).hasSize(3);
    }

    @Test
    void filterByPartialName() {
        when(loadBandsPort.loadAll()).thenReturn(List.of(metallica, pinkFloyd, acDc));

        List<Band> result = bandService.find("pink", BandSort.NAME, SortDirection.ASC);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Pink Floyd");
    }

    @Test
    void searchIsCaseInsensitive() {
        when(loadBandsPort.loadAll()).thenReturn(List.of(metallica, pinkFloyd, acDc));

        List<Band> result = bandService.find("PINK", BandSort.NAME, SortDirection.ASC);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Pink Floyd");
    }

    @Test
    void trimSearchInput() {
        when(loadBandsPort.loadAll()).thenReturn(List.of(metallica, pinkFloyd, acDc));

        List<Band> result = bandService.find("  pink  ", BandSort.NAME, SortDirection.ASC);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Pink Floyd");
    }

    @Test
    void sortByNameAscending() {
        when(loadBandsPort.loadAll()).thenReturn(List.of(metallica, pinkFloyd, acDc));

        List<Band> result = bandService.find(null, BandSort.NAME, SortDirection.ASC);

        assertThat(result).extracting(Band::name)
                .containsExactly("AC/DC", "Metallica", "Pink Floyd");
    }

    @Test
    void sortByNameDescending() {
        when(loadBandsPort.loadAll()).thenReturn(List.of(metallica, pinkFloyd, acDc));

        List<Band> result = bandService.find(null, BandSort.NAME, SortDirection.DESC);

        assertThat(result).extracting(Band::name)
                .containsExactly("Pink Floyd", "Metallica", "AC/DC");
    }

    @Test
    void sortByPopularityAscending() {
        when(loadBandsPort.loadAll()).thenReturn(List.of(metallica, pinkFloyd, acDc));

        List<Band> result = bandService.find(null, BandSort.POPULARITY, SortDirection.ASC);

        assertThat(result).extracting(Band::numPlays)
                .containsExactly(284_212L, 500_000L, 1_000_000L);
    }

    @Test
    void sortByPopularityDescending() {
        when(loadBandsPort.loadAll()).thenReturn(List.of(metallica, pinkFloyd, acDc));

        List<Band> result = bandService.find(null, BandSort.POPULARITY, SortDirection.DESC);

        assertThat(result).extracting(Band::numPlays)
                .containsExactly(1_000_000L, 500_000L, 284_212L);
    }

    @Test
    void getExistingBandById() {
        when(loadBandsPort.loadAll()).thenReturn(List.of(metallica, pinkFloyd, acDc));

        Band result = bandService.getById(ID_2);

        assertThat(result.name()).isEqualTo("Pink Floyd");
    }

    @Test
    void throwBandNotFoundExceptionForUnknownId() {
        when(loadBandsPort.loadAll()).thenReturn(List.of(metallica, pinkFloyd));

        UUID unknownId = UUID.fromString("99999999-9999-9999-9999-999999999999");

        assertThatThrownBy(() -> bandService.getById(unknownId))
                .isInstanceOf(BandNotFoundException.class);
    }

    @Test
    void returnImmutableResults() {
        when(loadBandsPort.loadAll()).thenReturn(List.of(metallica, pinkFloyd, acDc));

        List<Band> result = bandService.find(null, BandSort.NAME, SortDirection.ASC);

        assertThatThrownBy(() -> result.add(metallica))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void handleNullSearchAsNoFilter() {
        when(loadBandsPort.loadAll()).thenReturn(List.of(metallica, pinkFloyd, acDc));

        List<Band> result = bandService.find(null, BandSort.NAME, SortDirection.ASC);

        assertThat(result).hasSize(3);
    }

    @Test
    void handleBlankSearchAsNoFilter() {
        when(loadBandsPort.loadAll()).thenReturn(List.of(metallica, pinkFloyd, acDc));

        List<Band> result = bandService.find("   ", BandSort.NAME, SortDirection.ASC);

        assertThat(result).hasSize(3);
    }
}
