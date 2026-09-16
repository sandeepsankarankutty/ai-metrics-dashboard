package com.metrics.dashboard.exception;

/** Raised when HTML generation fails. */
public class GenerationException extends DashboardException {
    public GenerationException(String message) {
        super(message);
    }

    public GenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
