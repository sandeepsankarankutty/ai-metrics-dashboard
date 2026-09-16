package com.metrics.dashboard.service;

import com.metrics.dashboard.model.KPIData;
import com.metrics.dashboard.model.KPIStatus;
import com.metrics.dashboard.model.ProjectMetrics;
import com.metrics.dashboard.util.Constants;
import com.metrics.dashboard.util.ValidationUtils;
import java.util.List;

/** Validates portfolio KPI values against configured targets. */
public class KPIValidator {
    /** Builds the portfolio KPI set. */
    public List<KPIData> buildPortfolioKpis(List<ProjectMetrics> projects) {
        double totalTests = projects.stream().mapToDouble(ProjectMetrics::totalTestsGenerated).sum();
        double totalAiTests = projects.stream().mapToDouble(ProjectMetrics::aiGeneratedCount).sum();
        double productivityGain = weightedAverage(projects, ProjectMetrics::productivityGain, ProjectMetrics::aiGeneratedCount);
        double reviewReduction = weightedAverage(projects, ProjectMetrics::reviewEffortReduction, ProjectMetrics::aiGeneratedCount);
        double coverage = weightedAverage(projects, ProjectMetrics::requirementCoverage, ProjectMetrics::totalTestsGenerated);
        double automation = weightedAverage(projects, ProjectMetrics::automationCandidatePercentage, ProjectMetrics::aiGeneratedCount);
        double adoption = ValidationUtils.safeDivide(totalAiTests, totalTests) * 100.0d;

        return List.of(
                new KPIData("Test Design Productivity Gain", ValidationUtils.round(productivityGain),
                        Constants.PRODUCTIVITY_GAIN_TARGET,
                        determineStatus(productivityGain, Constants.PRODUCTIVITY_GAIN_TARGET),
                        determineStatus(productivityGain, Constants.PRODUCTIVITY_GAIN_TARGET).getCssClass(),
                        "Target: +30% annual improvement", "%"),
                new KPIData("AI Adoption", ValidationUtils.round(adoption), Constants.AI_ADOPTION_TARGET,
                        determineStatus(adoption, Constants.AI_ADOPTION_TARGET),
                        determineStatus(adoption, Constants.AI_ADOPTION_TARGET).getCssClass(),
                        "Target: 60-80% of tests AI-assisted", "%"),
                new KPIData("Review Effort Reduction", ValidationUtils.round(reviewReduction),
                        Constants.REVIEW_REDUCTION_TARGET,
                        determineStatus(reviewReduction, Constants.REVIEW_REDUCTION_TARGET),
                        determineStatus(reviewReduction, Constants.REVIEW_REDUCTION_TARGET).getCssClass(),
                        "Target: 25-40% less review effort", "%"),
                new KPIData("Requirement Coverage", ValidationUtils.round(coverage),
                        Constants.REQUIREMENT_COVERAGE_TARGET,
                        determineStatus(coverage, Constants.REQUIREMENT_COVERAGE_TARGET),
                        determineStatus(coverage, Constants.REQUIREMENT_COVERAGE_TARGET).getCssClass(),
                        "Target: 95% or better coverage", "%"),
                new KPIData("Automation Readiness", ValidationUtils.round(automation),
                        Constants.AUTOMATION_READINESS_TARGET,
                        determineStatus(automation, Constants.AUTOMATION_READINESS_TARGET),
                        determineStatus(automation, Constants.AUTOMATION_READINESS_TARGET).getCssClass(),
                        "Target: 50% automation-ready candidates", "%"));
    }

    /** Returns the KPI status for a target threshold. */
    public KPIStatus determineStatus(double currentValue, double targetValue) {
        if (currentValue >= targetValue) {
            return KPIStatus.GREEN;
        }
        if (currentValue >= targetValue * 0.75d) {
            return KPIStatus.YELLOW;
        }
        return KPIStatus.RED;
    }

    private double weightedAverage(List<ProjectMetrics> projects,
            java.util.function.ToDoubleFunction<ProjectMetrics> valueFunction,
            java.util.function.ToDoubleFunction<ProjectMetrics> weightFunction) {
        double weightedSum = 0.0d;
        double totalWeight = 0.0d;
        for (ProjectMetrics project : projects) {
            double weight = weightFunction.applyAsDouble(project);
            weightedSum += valueFunction.applyAsDouble(project) * weight;
            totalWeight += weight;
        }
        return ValidationUtils.safeDivide(weightedSum, totalWeight);
    }
}
