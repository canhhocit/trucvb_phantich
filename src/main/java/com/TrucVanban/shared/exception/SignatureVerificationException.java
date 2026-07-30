package com.TrucVanban.shared.exception;

public class SignatureVerificationException extends RuntimeException {
    public SignatureVerificationException(String message) {
        super(message);
    }
}
