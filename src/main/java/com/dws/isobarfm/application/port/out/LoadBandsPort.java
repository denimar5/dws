package com.dws.isobarfm.application.port.out;

import com.dws.isobarfm.domain.model.Band;

import java.util.List;

public interface LoadBandsPort {
    List<Band> loadAll();
}
