package com.dws.isobarfm.adapter.out.bandsapi.exception;

public class BandsProviderInvalidResponseException extends RuntimeException {

    public BandsProviderInvalidResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public BandsProviderInvalidResponseException(String message) {
        super(message);
    }
}
