package com.dws.isobarfm.adapter.out.bandsapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BandsApiResponse(
        String id,
        String name,
        String image,
        String genre,
        String biography,
        @JsonProperty("numPlays") long numPlays,
        List<String> albums
) {}
