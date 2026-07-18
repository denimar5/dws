package com.dws.isobarfm.adapter.out.bandsapi.exception;

public class BandsProviderUnavailableException extends RuntimeException {

    public BandsProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public BandsProviderUnavailableException(String message) {
        super(message);
    }
}
