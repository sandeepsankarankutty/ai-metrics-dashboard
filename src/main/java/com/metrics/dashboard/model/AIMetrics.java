package com.metrics.dashboard.model;

import com.metrics.dashboard.util.ValidationUtils;

/** AI-assisted testing metrics for a project. */
public record AIMetrics(
        String projectName,
        String lead,
        double analysisHours,
        double testCaseCreationHours,
        double storiesAnalyzed,
        double aiTotalInteractionTime,
        double generatedTestCases,
        double testCaseCoverage,
        double automationCandidatesIdentified,
        double reviewDefectsFound,
        double reviewTimePerTestCase) {

    public double estimatedTotalHours() {
        double detailedHours = analysisHours + testCaseCreationHours + (reviewTimePerTestCase * generatedTestCases);
        double interactionHours = aiTotalInteractionTime + (reviewTimePerTestCase * generatedTestCases);
        return Math.max(detailedHours, interactionHours);
    }

    public double productivityPerHour() {
        return ValidationUtils.safeDivide(generatedTestCases, estimatedTotalHours());
    }

    public double automationCandidatePercentage() {
        return ValidationUtils.safeDivide(automationCandidatesIdentified, generatedTestCases) * 100.0d;
    }
}
