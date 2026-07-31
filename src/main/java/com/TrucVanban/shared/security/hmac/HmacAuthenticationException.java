package com.TrucVanban.shared.security.hmac;

public abstract class HmacAuthenticationException extends RuntimeException {

    protected HmacAuthenticationException(String message) {
        super(message);
    }

    public static class MissingAuthHeaderException extends HmacAuthenticationException {
        public MissingAuthHeaderException(String message) {
            super(message);
        }
    }

    public static class TimestampSkewException extends HmacAuthenticationException {
        public TimestampSkewException(String message) {
            super(message);
        }
    }

    public static class ApiKeyInvalidException extends HmacAuthenticationException {
        public ApiKeyInvalidException(String message) {
            super(message);
        }
    }

    public static class ApiKeyExpiredException extends HmacAuthenticationException {
        public ApiKeyExpiredException(String message) {
            super(message);
        }
    }

    public static class AgencyInactiveException extends HmacAuthenticationException {
        public AgencyInactiveException(String message) {
            super(message);
        }
    }

    public static class SignatureInvalidException extends HmacAuthenticationException {
        public SignatureInvalidException(String message) {
            super(message);
        }
    }

    public static class ReplayDetectedException extends HmacAuthenticationException {
        public ReplayDetectedException(String message) {
            super(message);
        }
    }

    public static class AuthStoreUnavailableException extends HmacAuthenticationException {
        public AuthStoreUnavailableException(String message) {
            super(message);
        }
    }
}
