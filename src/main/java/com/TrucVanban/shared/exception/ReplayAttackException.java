package com.TrucVanban.shared.exception;

public class ReplayAttackException extends RuntimeException {
    public ReplayAttackException(String message) {
        super(message);
    }
}
