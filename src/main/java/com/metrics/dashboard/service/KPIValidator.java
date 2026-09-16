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
        double totalTests = projects.stream().mapToDouble(ProjectMetrics::totalTestsBefore).sum();
        double totalAiTests = projects.stream().mapToDouble(ProjectMetrics::aiGeneratedCount).sum();
        double productivityGain = calculatePortfolioGain(projects);
        double reviewReduction = weightedAverage(projects, ProjectMetrics::reviewEffortReduction, ProjectMetrics::aiGeneratedCount);
        double coverage = weightedAverage(projects, ProjectMetrics::requirementCoverage, ProjectMetrics::totalTestsBefore);
        double automation = weightedAverage(projects, ProjectMetrics::automationCandidatePercentage, ProjectMetrics::totalTestsBefore);
        double adoption = ValidationUtils.safeDivide(totalAiTests, totalTests) * 100.0d;

        KPIStatus productivityStatus = determineStatus(productivityGain, Constants.PRODUCTIVITY_GAIN_TARGET);
        KPIStatus adoptionStatus = determineStatus(adoption, Constants.AI_ADOPTION_TARGET);
        KPIStatus reviewStatus = determineStatus(reviewReduction, Constants.REVIEW_REDUCTION_TARGET);
        KPIStatus coverageStatus = determineStatus(coverage, Constants.REQUIREMENT_COVERAGE_TARGET);
        KPIStatus automationStatus = determineStatus(automation, Constants.AUTOMATION_READINESS_TARGET);

        return List.of(
                new KPIData("Test Design Productivity Gain", ValidationUtils.round(productivityGain),
                        Constants.PRODUCTIVITY_GAIN_TARGET, productivityStatus, productivityStatus.getCssClass(),
                        "Target: +30% annual improvement", "%"),
                new KPIData("AI Adoption Rate", ValidationUtils.round(adoption), Constants.AI_ADOPTION_TARGET, adoptionStatus,
                        adoptionStatus.getCssClass(),
                        "Target: 60-80% of tests AI-assisted", "%"),
                new KPIData("Review Effort Reduction", ValidationUtils.round(reviewReduction),
                        Constants.REVIEW_REDUCTION_TARGET, reviewStatus, reviewStatus.getCssClass(),
                        "Target: 25-40% less review effort", "%"),
                new KPIData("Requirement Coverage", ValidationUtils.round(coverage),
                        Constants.REQUIREMENT_COVERAGE_TARGET, coverageStatus, coverageStatus.getCssClass(),
                        "Target: 95% or better coverage", "%"),
                new KPIData("Automation Readiness", ValidationUtils.round(automation),
                        Constants.AUTOMATION_READINESS_TARGET, automationStatus, automationStatus.getCssClass(),
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

    private double calculatePortfolioGain(List<ProjectMetrics> projects) {
        double totalAiCases = projects.stream().mapToDouble(ProjectMetrics::aiGeneratedCount).sum();
        double baselineComparableHours = projects.stream()
                .mapToDouble(project -> ValidationUtils.safeDivide(
                        project.baselineMetrics().totalCoreHours(),
                        project.baselineMetrics().totalTestCasesPerMonth()) * project.aiGeneratedCount())
                .sum();
        double aiHours = projects.stream().mapToDouble(project -> project.aiMetrics().estimatedTotalHours()).sum();
        double beforeRate = ValidationUtils.safeDivide(totalAiCases, baselineComparableHours);
        double afterRate = ValidationUtils.safeDivide(totalAiCases, aiHours);
        return beforeRate == 0.0d ? 0.0d : ((afterRate - beforeRate) / beforeRate) * 100.0d;
    }
}
