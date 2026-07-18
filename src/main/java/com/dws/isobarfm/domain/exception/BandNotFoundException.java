package com.dws.isobarfm.domain.exception;

import java.util.UUID;

public class BandNotFoundException extends RuntimeException {

    private final UUID bandId;

    public BandNotFoundException(UUID bandId) {
        super("Band with identifier " + bandId + " was not found.");
        this.bandId = bandId;
    }

    public UUID getBandId() {
        return bandId;
    }
}
