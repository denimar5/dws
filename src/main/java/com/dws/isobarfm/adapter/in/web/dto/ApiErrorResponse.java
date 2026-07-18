package com.dws.isobarfm.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record ApiErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path
) {}
