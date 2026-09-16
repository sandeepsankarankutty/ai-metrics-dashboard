package com.metrics.dashboard.model;

import com.metrics.dashboard.util.Constants;
import com.metrics.dashboard.util.ValidationUtils;

/** Baseline manual testing metrics for a project. */
public record BaselineMetrics(
        String projectName,
        String lead,
        double analysisHours,
        double testCaseCreationHours,
        double totalTestCasesPerMonth,
        double requirementCoverage,
        double defectsFound,
        double reviewHoursPer100Cases,
        double reworkRate) {

    public double estimatedTotalHours() {
        double reviewHours = (reviewHoursPer100Cases / 100.0d) * totalTestCasesPerMonth;
        double reworkHours = testCaseCreationHours * (reworkRate / 100.0d);
        return analysisHours + testCaseCreationHours + reviewHours + reworkHours;
    }

    public double productivityPerHour() {
        return ValidationUtils.safeDivide(totalTestCasesPerMonth, estimatedTotalHours());
    }

    public double productivityPerDay() {
        return productivityPerHour() * Constants.HOURS_PER_QA_DAY;
    }

    public double defectYield() {
        return ValidationUtils.safeDivide(defectsFound, totalTestCasesPerMonth) * 100.0d;
    }
}
