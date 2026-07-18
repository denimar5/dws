package com.dws.isobarfm.adapter.in.web.error;

import com.dws.isobarfm.adapter.in.web.BandController;
import com.dws.isobarfm.adapter.in.web.dto.ApiErrorResponse;
import com.dws.isobarfm.adapter.out.bandsapi.exception.BandsProviderInvalidResponseException;
import com.dws.isobarfm.adapter.out.bandsapi.exception.BandsProviderUnavailableException;
import com.dws.isobarfm.domain.exception.BandNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BandNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleBandNotFound(BandNotFoundException ex,
                                                               HttpServletRequest request) {
        log.info("Band not found: {}", ex.getBandId());
        return buildResponse(HttpStatus.NOT_FOUND, "BAND_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(BandController.InvalidSortParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidSort(
            BandController.InvalidSortParameterException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_SORT", ex.getMessage(), request);
    }

    @ExceptionHandler(BandController.InvalidDirectionParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidDirection(
            BandController.InvalidDirectionParameterException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_DIRECTION", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Invalid parameter: " + ex.getName(), request);
    }

    @ExceptionHandler(BandsProviderUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleProviderUnavailable(
            BandsProviderUnavailableException ex, HttpServletRequest request) {
        log.warn("Bands provider unavailable: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "BANDS_PROVIDER_UNAVAILABLE",
                "The bands provider is currently unavailable.", request);
    }

    @ExceptionHandler(BandsProviderInvalidResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleProviderInvalidResponse(
            BandsProviderInvalidResponseException ex, HttpServletRequest request) {
        log.warn("Bands provider invalid response: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, "BANDS_PROVIDER_INVALID_RESPONSE",
                "The bands provider returned an invalid response.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error processing request", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred.", request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String code,
                                                           String message, HttpServletRequest request) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
