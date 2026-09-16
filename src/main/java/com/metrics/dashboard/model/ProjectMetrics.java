package com.metrics.dashboard.model;

/** Aggregated project-level metrics for dashboard rendering. */
public record ProjectMetrics(
        String projectName,
        String lead,
        BaselineMetrics baselineMetrics,
        AIMetrics aiMetrics,
        double totalTestsBefore,
        double totalTestsGenerated,
        double aiGeneratedCount,
        double baselineProductivityPerHour,
        double baselineProductivityPerDay,
        double aiProductivityPerHour,
        double productivityGain,
        double aiAdoptionRate,
        double reviewEffortReduction,
        double requirementCoverage,
        double automationCandidatePercentage,
        double defectDensityBefore,
        double defectDensityAfter,
        double qualityImprovement,
        double hoursSaved,
        double costAvoidance,
        String maturityLevel) {

    public static ProjectMetrics seed(BaselineMetrics baselineMetrics, AIMetrics aiMetrics) {
        String projectName = baselineMetrics != null ? baselineMetrics.projectName() : aiMetrics.projectName();
        String lead = baselineMetrics != null ? baselineMetrics.lead() : aiMetrics.lead();
        return new ProjectMetrics(
                projectName,
                lead,
                baselineMetrics,
                aiMetrics,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                "Beginner");
    }
}
