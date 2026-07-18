package com.dws.isobarfm.application.port.in;

import com.dws.isobarfm.domain.model.Band;

import java.util.UUID;

public interface GetBandDetailsUseCase {
    Band getById(UUID id);
}
