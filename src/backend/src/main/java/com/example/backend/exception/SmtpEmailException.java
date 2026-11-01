package com.example.backend.exception;

public class SmtpEmailException extends RuntimeException {

    public enum Reason {
        CONFIGURATION,
        AUTHENTICATION,
        CONNECTION,
        TIMEOUT,
        DELIVERY
    }

    private final Reason reason;

    public SmtpEmailException(Reason reason) {
        super(safeMessage(reason));
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    private static String safeMessage(Reason reason) {
        return switch (reason) {
            case CONFIGURATION -> "SMTP configuration is incomplete";
            case AUTHENTICATION -> "SMTP authentication failed";
            case CONNECTION -> "SMTP connection failed";
            case TIMEOUT -> "SMTP operation timed out";
            case DELIVERY -> "SMTP delivery failed";
        };
    }
}
