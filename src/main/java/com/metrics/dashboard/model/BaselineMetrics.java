package com.metrics.dashboard.model;

import com.metrics.dashboard.util.Constants;
import com.metrics.dashboard.util.ValidationUtils;

/** Baseline manual testing metrics for a project. */
public record BaselineMetrics(
        String projectName,
        String lead,
        double analysisHoursComplexBS,
        double analysisHoursHighSP,
        double analysisHoursMediumSP,
        double analysisHoursLowerSP,
        double designHoursComplexBS,
        double designHoursHighSP,
        double designHoursMediumSP,
        double designHoursLowerSP,
        double totalTestCasesPerMonth,
        double requirementCoveragePercent,
        double defectsFound,
        double reviewHoursPer100Cases,
        double reworkPercentage) {

    public double analysisHours() {
        return analysisHoursComplexBS + analysisHoursHighSP + analysisHoursMediumSP + analysisHoursLowerSP;
    }

    public double testCaseCreationHours() {
        return designHoursComplexBS + designHoursHighSP + designHoursMediumSP + designHoursLowerSP;
    }

    public double totalCoreHours() {
        return analysisHours() + testCaseCreationHours();
    }

    public double estimatedTotalHours() {
        return totalCoreHours();
    }

    public double reviewEffortBeforeHours() {
        return (totalTestCasesPerMonth * reviewHoursPer100Cases) / 100.0d;
    }

    public double defectDensity() {
        return ValidationUtils.safeDivide(defectsFound, totalTestCasesPerMonth);
    }

    public double productivityPerHour() {
        return ValidationUtils.safeDivide(totalTestCasesPerMonth, totalCoreHours());
    }

    public double productivityPerDay() {
        return productivityPerHour() * Constants.HOURS_PER_QA_DAY;
    }

    public double defectYield() {
        return defectDensity() * 100.0d;
    }
}
