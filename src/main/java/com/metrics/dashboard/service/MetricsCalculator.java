package com.metrics.dashboard.service;

import com.metrics.dashboard.model.AIProductivitySummary;
import com.metrics.dashboard.model.AIMetrics;
import com.metrics.dashboard.model.BaselineMetrics;
import com.metrics.dashboard.model.BaselineSummary;
import com.metrics.dashboard.model.DashboardData;
import com.metrics.dashboard.model.ExecutiveSummary;
import com.metrics.dashboard.model.GovernanceSummary;
import com.metrics.dashboard.model.KPIData;
import com.metrics.dashboard.model.ProjectMetrics;
import com.metrics.dashboard.util.Constants;
import com.metrics.dashboard.util.ValidationUtils;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Calculates project, executive, governance, and chart metrics. */
public class MetricsCalculator {
    private final KPIValidator kpiValidator;

    public MetricsCalculator() {
        this(new KPIValidator());
    }

    public MetricsCalculator(KPIValidator kpiValidator) {
        this.kpiValidator = kpiValidator;
    }

    /** Builds the dashboard data model from parsed project metrics. */
    public DashboardData buildDashboardData(List<ProjectMetrics> parsedProjects) {
        List<ProjectMetrics> calculatedProjects = parsedProjects.stream()
                .map(this::calculateProject)
                .collect(Collectors.toList());

        BaselineSummary baselineSummary = createBaselineSummary(calculatedProjects);
        AIProductivitySummary aiSummary = createAiSummary(calculatedProjects);
        ExecutiveSummary executiveSummary = createExecutiveSummary(calculatedProjects);
        GovernanceSummary governanceSummary = createGovernanceSummary(calculatedProjects, executiveSummary);
        List<KPIData> kpis = kpiValidator.buildPortfolioKpis(calculatedProjects);

        return new DashboardData(
                baselineSummary,
                aiSummary,
                executiveSummary,
                governanceSummary,
                kpis,
                calculatedProjects,
                buildCharts(calculatedProjects),
                OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    private ProjectMetrics calculateProject(ProjectMetrics seed) {
        BaselineMetrics baseline = seed.baselineMetrics();
        AIMetrics ai = seed.aiMetrics();
        double totalTests = Math.max(baseline.totalTestCasesPerMonth(), ai.generatedTestCases());
        double baselinePerHour = baseline.productivityPerHour();
        double baselinePerDay = baseline.productivityPerDay();
        double aiPerHour = ai.productivityPerHour();
        double productivityGain = baselinePerHour == 0.0d ? 0.0d : ((aiPerHour - baselinePerHour) / baselinePerHour) * 100.0d;
        double adoption = ValidationUtils.safeDivide(ai.generatedTestCases(), totalTests) * 100.0d;
        double baselineReviewPerCase = baseline.reviewHoursPer100Cases() / 100.0d;
        double reviewReduction = baselineReviewPerCase == 0.0d
                ? 0.0d
                : ((baselineReviewPerCase - ai.reviewTimePerTestCase()) / baselineReviewPerCase) * 100.0d;
        double coverage = ai.testCaseCoverage() > 0.0d ? ai.testCaseCoverage() : baseline.requirementCoverage();
        double automation = ai.automationCandidatePercentage();
        double baselineHoursForAiVolume = ValidationUtils.safeDivide(baseline.estimatedTotalHours(), baseline.totalTestCasesPerMonth())
                * ai.generatedTestCases();
        double hoursSaved = Math.max(baselineHoursForAiVolume - ai.estimatedTotalHours(), 0.0d);
        double costAvoidance = hoursSaved * Constants.QA_HOURLY_RATE;

        return new ProjectMetrics(
                seed.projectName(),
                seed.lead(),
                baseline,
                ai,
                ValidationUtils.round(totalTests),
                ValidationUtils.round(ai.generatedTestCases()),
                ValidationUtils.round(baselinePerHour),
                ValidationUtils.round(baselinePerDay),
                ValidationUtils.round(aiPerHour),
                ValidationUtils.round(productivityGain),
                ValidationUtils.round(adoption),
                ValidationUtils.round(reviewReduction),
                ValidationUtils.round(coverage),
                ValidationUtils.round(automation),
                ValidationUtils.round(hoursSaved),
                ValidationUtils.round(costAvoidance),
                determineMaturityLevel(productivityGain));
    }

    private BaselineSummary createBaselineSummary(List<ProjectMetrics> projects) {
        double totalCases = projects.stream().mapToDouble(p -> p.baselineMetrics().totalTestCasesPerMonth()).sum();
        double productivityPerDay = weightedAverage(projects, ProjectMetrics::baselineProductivityPerDay,
                p -> p.baselineMetrics().totalTestCasesPerMonth());
        double coverage = weightedAverage(projects, p -> p.baselineMetrics().requirementCoverage(),
                p -> p.baselineMetrics().totalTestCasesPerMonth());
        double defectYield = weightedAverage(projects, p -> p.baselineMetrics().defectYield(),
                p -> p.baselineMetrics().totalTestCasesPerMonth());
        double reworkRate = weightedAverage(projects, p -> p.baselineMetrics().reworkRate(),
                p -> p.baselineMetrics().totalTestCasesPerMonth());
        double reviewEffort = weightedAverage(projects, p -> p.baselineMetrics().reviewHoursPer100Cases(),
                p -> p.baselineMetrics().totalTestCasesPerMonth());
        return new BaselineSummary(
                ValidationUtils.round(totalCases),
                ValidationUtils.round(productivityPerDay),
                ValidationUtils.round(coverage),
                ValidationUtils.round(defectYield),
                ValidationUtils.round(reworkRate),
                ValidationUtils.round(reviewEffort));
    }

    private AIProductivitySummary createAiSummary(List<ProjectMetrics> projects) {
        double totalAiCases = projects.stream().mapToDouble(ProjectMetrics::aiGeneratedCount).sum();
        double testsPerHourBefore = weightedAverage(projects, ProjectMetrics::baselineProductivityPerHour,
                ProjectMetrics::aiGeneratedCount);
        double testsPerHourAfter = weightedAverage(projects, ProjectMetrics::aiProductivityPerHour,
                ProjectMetrics::aiGeneratedCount);
        double productivityGain = weightedAverage(projects, ProjectMetrics::productivityGain, ProjectMetrics::aiGeneratedCount);
        double adoption = weightedAverage(projects, ProjectMetrics::aiAdoptionRate, ProjectMetrics::totalTestsGenerated);
        return new AIProductivitySummary(
                ValidationUtils.round(totalAiCases),
                ValidationUtils.round(testsPerHourBefore),
                ValidationUtils.round(testsPerHourAfter),
                ValidationUtils.round(productivityGain),
                ValidationUtils.round(adoption));
    }

    private ExecutiveSummary createExecutiveSummary(List<ProjectMetrics> projects) {
        double totalStories = projects.stream().mapToDouble(p -> p.aiMetrics().storiesAnalyzed()).sum();
        double totalTests = projects.stream().mapToDouble(ProjectMetrics::totalTestsGenerated).sum();
        double aiGenerated = projects.stream().mapToDouble(ProjectMetrics::aiGeneratedCount).sum();
        double aiPct = ValidationUtils.safeDivide(aiGenerated, totalTests) * 100.0d;
        double overallGain = weightedAverage(projects, ProjectMetrics::productivityGain, ProjectMetrics::aiGeneratedCount);
        double hoursSaved = projects.stream().mapToDouble(ProjectMetrics::hoursSaved).sum();
        double costAvoidance = projects.stream().mapToDouble(ProjectMetrics::costAvoidance).sum();
        return new ExecutiveSummary(
                ValidationUtils.round(totalStories),
                ValidationUtils.round(totalTests),
                ValidationUtils.round(aiGenerated),
                ValidationUtils.round(aiPct),
                ValidationUtils.round(overallGain),
                ValidationUtils.round(hoursSaved),
                ValidationUtils.round(costAvoidance));
    }

    private GovernanceSummary createGovernanceSummary(List<ProjectMetrics> projects, ExecutiveSummary executiveSummary) {
        double coverage = weightedAverage(projects, ProjectMetrics::requirementCoverage, ProjectMetrics::totalTestsGenerated);
        double qualityScore = weightedAverage(projects,
                p -> (p.requirementCoverage() + p.reviewEffortReduction() + Math.min(100.0d, p.automationCandidatePercentage())) / 3.0d,
                ProjectMetrics::aiGeneratedCount);
        double coverageTrend = coverage >= Constants.REQUIREMENT_COVERAGE_TARGET ? 1.0d : -1.0d;
        double reviewTrend = weightedAverage(projects, ProjectMetrics::reviewEffortReduction, ProjectMetrics::aiGeneratedCount) >= Constants.REVIEW_REDUCTION_TARGET ? 1.0d : -1.0d;
        double productivityTrend = executiveSummary.overallProductivityGain() >= Constants.PRODUCTIVITY_GAIN_TARGET ? 1.0d : -1.0d;
        return new GovernanceSummary(
                determineMaturityLevel(executiveSummary.overallProductivityGain()),
                ValidationUtils.round(qualityScore),
                productivityTrend,
                coverageTrend,
                reviewTrend);
    }

    private Map<String, Object> buildCharts(List<ProjectMetrics> projects) {
        Map<String, Object> charts = new LinkedHashMap<>();
        List<String> labels = projects.stream().map(ProjectMetrics::projectName).collect(Collectors.toList());
        charts.put("labels", labels);
        charts.put("baselineProductivity", projects.stream().map(ProjectMetrics::baselineProductivityPerHour).toList());
        charts.put("aiProductivity", projects.stream().map(ProjectMetrics::aiProductivityPerHour).toList());
        charts.put("adoptionRates", projects.stream().map(ProjectMetrics::aiAdoptionRate).toList());
        charts.put("coverageValues", projects.stream().map(ProjectMetrics::requirementCoverage).toList());
        charts.put("automationValues", projects.stream().map(ProjectMetrics::automationCandidatePercentage).toList());
        charts.put("costAvoidanceValues", projects.stream().map(ProjectMetrics::costAvoidance).toList());
        return charts;
    }

    private String determineMaturityLevel(double productivityGain) {
        if (productivityGain >= 80.0d) {
            return "Agentic";
        }
        if (productivityGain >= 50.0d) {
            return "Advanced";
        }
        if (productivityGain >= 30.0d) {
            return "Intermediate";
        }
        return "Beginner";
    }

    private double weightedAverage(List<ProjectMetrics> projects,
            java.util.function.ToDoubleFunction<ProjectMetrics> value,
            java.util.function.ToDoubleFunction<ProjectMetrics> weight) {
        double numerator = 0.0d;
        double denominator = 0.0d;
        for (ProjectMetrics project : projects) {
            double currentWeight = weight.applyAsDouble(project);
            numerator += value.applyAsDouble(project) * currentWeight;
            denominator += currentWeight;
        }
        return ValidationUtils.safeDivide(numerator, denominator);
    }
}
