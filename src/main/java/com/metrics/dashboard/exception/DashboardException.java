package com.metrics.dashboard.exception;

/** Base exception for dashboard generation failures. */
public class DashboardException extends RuntimeException {
    public DashboardException(String message) {
        super(message);
    }

    public DashboardException(String message, Throwable cause) {
        super(message, cause);
    }
}
