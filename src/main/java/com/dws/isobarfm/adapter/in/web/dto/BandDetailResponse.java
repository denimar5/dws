package com.dws.isobarfm.adapter.in.web.dto;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public record BandDetailResponse(
        UUID id,
        String name,
        URI image,
        String genre,
        String biography,
        long numPlays,
        List<UUID> albumIds
) {}
