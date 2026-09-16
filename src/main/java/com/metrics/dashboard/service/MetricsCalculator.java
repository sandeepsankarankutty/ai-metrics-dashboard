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

        double baselineCases = baseline.totalTestCasesPerMonth();
        double aiGenerated = ai.generatedTestCases();
        double totalAfter = ai.totalTestCasesGenerated() > 0.0d ? ai.totalTestCasesGenerated() : aiGenerated;

        double baselineProductivity = baseline.productivityPerHour();
        double baselinePerDay = baseline.productivityPerDay();
        double aiProductivity = ai.productivityPerHour();
        double productivityGain = baselineProductivity == 0.0d
                ? 0.0d
                : ((aiProductivity - baselineProductivity) / baselineProductivity) * 100.0d;

        double reviewBefore = baseline.reviewEffortBeforeHours();
        double reviewAfter = ai.reviewEffortAfterHours();
        double reviewReduction = reviewBefore == 0.0d ? 0.0d : ((reviewBefore - reviewAfter) / reviewBefore) * 100.0d;

        double adoption = ValidationUtils.safeDivide(aiGenerated, totalAfter > 0.0d ? totalAfter : baselineCases) * 100.0d;
        double automation = ValidationUtils.safeDivide(ai.automationCandidates(), aiGenerated) * 100.0d;
        double coverage = ai.coveragePercentage() > 0.0d ? ai.coveragePercentage() : baseline.requirementCoveragePercent();

        double baselineHoursForAiVolume = ValidationUtils.safeDivide(baseline.totalEffortHours(), baselineCases) * aiGenerated;
        double hoursSaved = Math.max(baselineHoursForAiVolume - ai.totalAiHours(), 0.0d);
        double costAvoidance = hoursSaved * Constants.QA_HOURLY_RATE;

        double defectDensityBefore = baseline.defectDensity();
        double defectDensityAfter = ai.defectDensity();
        double qualityImprovement = defectDensityBefore == 0.0d
                ? 0.0d
                : ((defectDensityBefore - defectDensityAfter) / defectDensityBefore) * 100.0d;

        return new ProjectMetrics(
                seed.projectName(),
                seed.lead(),
                baseline,
                ai,
                ValidationUtils.round(baselineCases),
                ValidationUtils.round(totalAfter),
                ValidationUtils.round(aiGenerated),
                ValidationUtils.round(baselineProductivity),
                ValidationUtils.round(baselinePerDay),
                ValidationUtils.round(aiProductivity),
                ValidationUtils.round(productivityGain),
                ValidationUtils.round(adoption),
                ValidationUtils.round(reviewReduction),
                ValidationUtils.round(coverage),
                ValidationUtils.round(automation),
                ValidationUtils.round(defectDensityBefore),
                ValidationUtils.round(defectDensityAfter),
                ValidationUtils.round(qualityImprovement),
                ValidationUtils.round(hoursSaved),
                ValidationUtils.round(costAvoidance),
                determineMaturityLevel(productivityGain));
    }

    private BaselineSummary createBaselineSummary(List<ProjectMetrics> projects) {
        double totalCases = projects.stream().mapToDouble(p -> p.baselineMetrics().totalTestCasesPerMonth()).sum();
        double productivityPerDay = weightedAverage(projects, ProjectMetrics::baselineProductivityPerDay,
                p -> p.baselineMetrics().totalTestCasesPerMonth());
        double coverage = weightedAverage(projects, p -> p.baselineMetrics().requirementCoveragePercent(),
                p -> p.baselineMetrics().totalTestCasesPerMonth());
        double defectYield = weightedAverage(projects, p -> p.baselineMetrics().defectYield(),
                p -> p.baselineMetrics().totalTestCasesPerMonth());
        double reworkRate = weightedAverage(projects, p -> p.baselineMetrics().reworkPercentage(),
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
        double testsPerHourBefore = calculatePortfolioBeforeRate(projects);
        double testsPerHourAfter = calculatePortfolioAfterRate(projects);
        double productivityGain = calculatePortfolioGain(projects);
        double adoption = weightedAverage(projects, ProjectMetrics::aiAdoptionRate,
                p -> p.totalTestsBefore() > 0.0d ? p.totalTestsBefore() : p.totalTestsGenerated());
        return new AIProductivitySummary(
                ValidationUtils.round(totalAiCases),
                ValidationUtils.round(testsPerHourBefore),
                ValidationUtils.round(testsPerHourAfter),
                ValidationUtils.round(productivityGain),
                ValidationUtils.round(adoption));
    }

    private ExecutiveSummary createExecutiveSummary(List<ProjectMetrics> projects) {
        double totalProjects = projects.size();
        double totalStories = projects.stream().mapToDouble(p -> p.aiMetrics().storiesAnalyzed()).sum();
        double totalBefore = projects.stream().mapToDouble(ProjectMetrics::totalTestsBefore).sum();
        double totalAfter = projects.stream().mapToDouble(ProjectMetrics::totalTestsGenerated).sum();
        double totalAi = projects.stream().mapToDouble(ProjectMetrics::aiGeneratedCount).sum();
        double adoption = ValidationUtils.safeDivide(totalAi, totalAfter) * 100.0d;
        double overallGain = calculatePortfolioGain(projects);
        double hoursSaved = projects.stream().mapToDouble(ProjectMetrics::hoursSaved).sum();
        double costAvoidance = projects.stream().mapToDouble(ProjectMetrics::costAvoidance).sum();
        double qualityImprovement = weightedAverage(projects, ProjectMetrics::qualityImprovement,
                p -> p.totalTestsBefore() > 0.0d ? p.totalTestsBefore() : p.totalTestsGenerated());
        double automationReadiness = weightedAverage(projects, ProjectMetrics::automationCandidatePercentage,
                p -> p.aiGeneratedCount() > 0.0d ? p.aiGeneratedCount() : p.totalTestsGenerated());
        return new ExecutiveSummary(
                ValidationUtils.round(totalProjects),
                ValidationUtils.round(totalStories),
                ValidationUtils.round(totalBefore),
                ValidationUtils.round(totalAfter),
                ValidationUtils.round(totalAi),
                ValidationUtils.round(adoption),
                ValidationUtils.round(overallGain),
                ValidationUtils.round(hoursSaved),
                ValidationUtils.round(costAvoidance),
                ValidationUtils.round(qualityImprovement),
                ValidationUtils.round(automationReadiness));
    }

    private GovernanceSummary createGovernanceSummary(List<ProjectMetrics> projects, ExecutiveSummary executiveSummary) {
        double coverage = weightedAverage(projects, ProjectMetrics::requirementCoverage,
                p -> p.totalTestsGenerated() > 0.0d ? p.totalTestsGenerated() : p.aiGeneratedCount());
        double qualityScore = weightedAverage(projects,
                p -> (p.requirementCoverage() + p.reviewEffortReduction() + p.qualityImprovement()) / 3.0d,
                p -> p.aiGeneratedCount() > 0.0d ? p.aiGeneratedCount() : p.totalTestsBefore());
        double coverageTrend = coverage >= Constants.REQUIREMENT_COVERAGE_TARGET ? 1.0d : -1.0d;
        double reviewTrend = weightedAverage(projects, ProjectMetrics::reviewEffortReduction, ProjectMetrics::aiGeneratedCount)
                >= Constants.REVIEW_REDUCTION_TARGET ? 1.0d : -1.0d;
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
        charts.put("productivityGainValues", projects.stream().map(ProjectMetrics::productivityGain).toList());
        charts.put("adoptionRates", projects.stream().map(ProjectMetrics::aiAdoptionRate).toList());
        charts.put("coverageBeforeValues", projects.stream().map(p -> p.baselineMetrics().requirementCoveragePercent()).toList());
        charts.put("coverageAfterValues", projects.stream().map(ProjectMetrics::requirementCoverage).toList());
        charts.put("automationValues", projects.stream().map(ProjectMetrics::automationCandidatePercentage).toList());
        charts.put("costAvoidanceValues", projects.stream().map(ProjectMetrics::costAvoidance).toList());
        charts.put("defectDensityBeforeValues", projects.stream().map(ProjectMetrics::defectDensityBefore).toList());
        charts.put("defectDensityAfterValues", projects.stream().map(ProjectMetrics::defectDensityAfter).toList());
        charts.put("reviewEffortBeforeValues", projects.stream().map(p -> p.baselineMetrics().reviewEffortBeforeHours()).toList());
        charts.put("reviewEffortAfterValues", projects.stream().map(p -> p.aiMetrics().reviewEffortAfterHours()).toList());
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
            if (currentWeight <= 0.0d) {
                continue;
            }
            numerator += value.applyAsDouble(project) * currentWeight;
            denominator += currentWeight;
        }
        return ValidationUtils.safeDivide(numerator, denominator);
    }

    private double calculatePortfolioBeforeRate(List<ProjectMetrics> projects) {
        double totalAiCases = projects.stream().mapToDouble(ProjectMetrics::aiGeneratedCount).sum();
        double baselineComparableHours = projects.stream()
                .mapToDouble(project -> ValidationUtils.safeDivide(
                        project.baselineMetrics().totalCoreHours(),
                        project.baselineMetrics().totalTestCasesPerMonth()) * project.aiGeneratedCount())
                .sum();
        return ValidationUtils.safeDivide(totalAiCases, baselineComparableHours);
    }

    private double calculatePortfolioAfterRate(List<ProjectMetrics> projects) {
        double totalAiCases = projects.stream().mapToDouble(ProjectMetrics::aiGeneratedCount).sum();
        double aiHours = projects.stream().mapToDouble(project -> project.aiMetrics().totalAiHours()).sum();
        return ValidationUtils.safeDivide(totalAiCases, aiHours);
    }

    private double calculatePortfolioGain(List<ProjectMetrics> projects) {
        double beforeRate = calculatePortfolioBeforeRate(projects);
        double afterRate = calculatePortfolioAfterRate(projects);
        return beforeRate == 0.0d ? 0.0d : ((afterRate - beforeRate) / beforeRate) * 100.0d;
    }
}
