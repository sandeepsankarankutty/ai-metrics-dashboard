package com.metrics.dashboard.model;

/** Portfolio-level AI productivity snapshot. */
public record AIProductivitySummary(
        double aiGeneratedTestCases,
        double testsPerHourBefore,
        double testsPerHourAfter,
        double productivityGain,
        double aiAdoptionRate) {
}
