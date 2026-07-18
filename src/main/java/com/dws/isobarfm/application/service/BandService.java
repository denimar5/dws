package com.dws.isobarfm.application.service;

import com.dws.isobarfm.application.port.in.FindBandsUseCase;
import com.dws.isobarfm.application.port.in.GetBandDetailsUseCase;
import com.dws.isobarfm.application.port.out.LoadBandsPort;
import com.dws.isobarfm.domain.exception.BandNotFoundException;
import com.dws.isobarfm.domain.model.Band;
import com.dws.isobarfm.domain.model.BandSort;
import com.dws.isobarfm.domain.model.SortDirection;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class BandService implements FindBandsUseCase, GetBandDetailsUseCase {

    private final LoadBandsPort loadBandsPort;

    public BandService(LoadBandsPort loadBandsPort) {
        this.loadBandsPort = loadBandsPort;
    }

    @Override
    public List<Band> find(String search, BandSort sort, SortDirection direction) {
        List<Band> bands = loadBandsPort.loadAll();

        if (search != null && !search.isBlank()) {
            String trimmed = search.trim().toLowerCase();
            bands = bands.stream()
                    .filter(b -> b.name().toLowerCase().contains(trimmed))
                    .toList();
        }

        Comparator<Band> comparator = buildComparator(sort);
        if (direction == SortDirection.DESC) {
            comparator = comparator.reversed();
        }

        return bands.stream().sorted(comparator).toList();
    }

    @Override
    public Band getById(UUID id) {
        return loadBandsPort.loadAll().stream()
                .filter(b -> b.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new BandNotFoundException(id));
    }

    private Comparator<Band> buildComparator(BandSort sort) {
        return switch (sort) {
            case NAME -> Comparator.comparing(Band::name, String.CASE_INSENSITIVE_ORDER);
            case POPULARITY -> Comparator.comparingLong(Band::numPlays);
        };
    }
}
