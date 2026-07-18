package com.dws.isobarfm.domain.model;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record Band(
        UUID id,
        String name,
        URI image,
        String genre,
        String biography,
        long numPlays,
        List<UUID> albumIds
) {
    public Band {
        albumIds = Collections.unmodifiableList(List.copyOf(albumIds));
    }
}
