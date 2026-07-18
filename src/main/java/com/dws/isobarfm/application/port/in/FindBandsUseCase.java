package com.dws.isobarfm.application.port.in;

import com.dws.isobarfm.domain.model.Band;
import com.dws.isobarfm.domain.model.BandSort;
import com.dws.isobarfm.domain.model.SortDirection;

import java.util.List;

public interface FindBandsUseCase {
    List<Band> find(String search, BandSort sort, SortDirection direction);
}
