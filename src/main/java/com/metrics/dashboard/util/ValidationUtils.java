package com.metrics.dashboard.util;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Utility methods for normalization and safe numeric calculations. */
public final class ValidationUtils {
    private ValidationUtils() {
    }

    /** Safely divides two numbers, returning zero when the denominator is zero. */
    public static double safeDivide(double numerator, double denominator) {
        return Math.abs(denominator) < 0.000001d ? 0.0d : numerator / denominator;
    }

    /** Normalizes an Excel header into a simple lookup token. */
    public static String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** Normalizes percentage-like values to 0-100. */
    public static double normalizePercentage(double value) {
        if (value > 0.0d && value <= 1.0d) {
            return value * 100.0d;
        }
        return value;
    }

    /** Rounds a value to two decimal places. */
    public static double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    /** Checks whether a normalized header contains all provided tokens. */
    public static boolean containsAllTokens(String normalizedHeader, String... tokens) {
        for (String token : tokens) {
            if (!normalizedHeader.contains(token)) {
                return false;
            }
        }
        return true;
    }

    /** Sums numeric values whose keys contain all tokens. */
    public static double sumMatching(Map<String, Double> values, String... tokens) {
        return values.entrySet().stream()
                .filter(entry -> containsAllTokens(entry.getKey(), tokens))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
    }
}
