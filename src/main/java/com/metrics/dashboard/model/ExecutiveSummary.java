package com.metrics.dashboard.model;

/** Executive summary values for portfolio reporting. */
public record ExecutiveSummary(
        double totalStories,
        double totalTestsGenerated,
        double aiGeneratedTests,
        double aiGeneratedPercentage,
        double overallProductivityGain,
        double hoursSaved,
        double costAvoidance) {
}
