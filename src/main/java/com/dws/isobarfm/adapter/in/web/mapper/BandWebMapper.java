package com.dws.isobarfm.adapter.in.web.mapper;

import com.dws.isobarfm.adapter.in.web.dto.BandDetailResponse;
import com.dws.isobarfm.adapter.in.web.dto.BandListResponse;
import com.dws.isobarfm.adapter.in.web.dto.BandSummaryResponse;
import com.dws.isobarfm.domain.model.Band;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BandWebMapper {

    public BandSummaryResponse toSummary(Band band) {
        return new BandSummaryResponse(band.id(), band.name(), band.image(), band.genre(), band.numPlays());
    }

    public BandDetailResponse toDetail(Band band) {
        return new BandDetailResponse(band.id(), band.name(), band.image(), band.genre(),
                band.biography(), band.numPlays(), band.albumIds());
    }

    public BandListResponse toListResponse(List<Band> bands) {
        List<BandSummaryResponse> items = bands.stream().map(this::toSummary).toList();
        return new BandListResponse(items.size(), items);
    }
}
