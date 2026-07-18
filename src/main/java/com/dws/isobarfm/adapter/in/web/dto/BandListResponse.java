package com.dws.isobarfm.adapter.in.web.dto;

import java.util.List;

public record BandListResponse(
        int count,
        List<BandSummaryResponse> items
) {}
