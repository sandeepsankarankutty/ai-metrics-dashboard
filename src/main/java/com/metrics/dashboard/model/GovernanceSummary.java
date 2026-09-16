package com.metrics.dashboard.model;

/** Governance and maturity summary for the portfolio. */
public record GovernanceSummary(
        String maturityLevel,
        double qualityScore,
        double productivityTrend,
        double coverageTrend,
        double reviewTrend) {
}
