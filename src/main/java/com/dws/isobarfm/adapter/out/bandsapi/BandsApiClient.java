package com.dws.isobarfm.adapter.out.bandsapi;

import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

public interface BandsApiClient {

    @GetExchange("/api/bands")
    List<BandsApiResponse> getBands();
}
