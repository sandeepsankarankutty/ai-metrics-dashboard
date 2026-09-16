package com.metrics.dashboard.exception;

/** Raised when the input workbook cannot be parsed. */
public class ExcelParsingException extends DashboardException {
    public ExcelParsingException(String message) {
        super(message);
    }

    public ExcelParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
