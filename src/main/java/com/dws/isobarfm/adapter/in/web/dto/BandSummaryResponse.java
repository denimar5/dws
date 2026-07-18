package com.dws.isobarfm.adapter.in.web.dto;

import java.net.URI;
import java.util.UUID;

public record BandSummaryResponse(
        UUID id,
        String name,
        URI image,
        String genre,
        long numPlays
) {}
