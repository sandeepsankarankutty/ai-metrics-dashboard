package com.metrics.dashboard.model;

/** Executive summary values for portfolio reporting. */
public record ExecutiveSummary(
        double totalProjects,
        double totalStoriesAnalyzed,
        double totalTestCasesBefore,
        double totalTestCasesAfter,
        double totalTestCasesAI,
        double aiAdoptionPercent,
        double overallProductivityGain,
        double totalHoursSaved,
        double totalCostAvoidance,
        double qualityImprovement,
        double automationReadiness) {
}
