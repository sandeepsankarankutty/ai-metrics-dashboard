package com.metrics.dashboard.model;

/** Portfolio-level baseline snapshot. */
public record BaselineSummary(
        double testCasesCreated,
        double productivityPerDay,
        double requirementCoverage,
        double defectYield,
        double reworkRate,
        double reviewEffort) {
}
