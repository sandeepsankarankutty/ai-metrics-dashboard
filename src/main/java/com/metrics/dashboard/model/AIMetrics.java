package com.metrics.dashboard.model;

import com.metrics.dashboard.util.ValidationUtils;

/** AI-assisted testing metrics for a project. */
public record AIMetrics(
        String projectName,
        String lead,
        double storiesAnalyzedComplex,
        double storiesAnalyzedHigh,
        double storiesAnalyzedMedium,
        double storiesAnalyzedLower,
        double aiTotalInteractionTime,
        double testCasesGeneratedComplex,
        double testCasesGeneratedHigh,
        double testCasesGeneratedMedium,
        double testCasesGeneratedLower,
        double totalTestCasesGenerated,
        double coveragePercentage,
        double automationCandidates,
        double reviewDefectsFound,
        double reviewTimePerTestCase,
        double reworkPercentage) {

    public double storiesAnalyzed() {
        return storiesAnalyzedComplex + storiesAnalyzedHigh + storiesAnalyzedMedium + storiesAnalyzedLower;
    }

    public double generatedTestCases() {
        double byBand = testCasesGeneratedComplex + testCasesGeneratedHigh + testCasesGeneratedMedium + testCasesGeneratedLower;
        return totalTestCasesGenerated > 0.0d ? totalTestCasesGenerated : byBand;
    }

    public double reviewEffortAfterHours() {
        return generatedTestCases() * reviewTimePerTestCase;
    }

    public double estimatedTotalHours() {
        return aiTotalInteractionTime + reviewEffortAfterHours();
    }

    public double totalAiHours() {
        return estimatedTotalHours();
    }

    public double productivityPerHour() {
        return ValidationUtils.safeDivide(generatedTestCases(), totalAiHours());
    }

    public double defectDensity() {
        return ValidationUtils.safeDivide(reviewDefectsFound, generatedTestCases());
    }
}
