package com.metrics.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.metrics.dashboard.model.AIMetrics;
import com.metrics.dashboard.model.BaselineMetrics;
import com.metrics.dashboard.model.DashboardData;
import com.metrics.dashboard.model.ProjectMetrics;
import java.util.List;
import org.junit.jupiter.api.Test;

class MetricsCalculatorTest {
    @Test
    void calculatesExpectedPortfolioMetrics() {
        BaselineMetrics baseline = new BaselineMetrics("CenAccess", "Alex", 1.0d, 4.0d, 30.0d, 96.0d, 5.0d, 10.0d, 25.0d);
        AIMetrics ai = new AIMetrics("CenAccess", "Alex", 0.5d, 0.3d, 10.0d, 0.8d, 30.0d, 97.0d, 18.0d, 1.0d, 0.03d);

        DashboardData dashboardData = new MetricsCalculator().buildDashboardData(List.of(ProjectMetrics.seed(baseline, ai)));
        ProjectMetrics project = dashboardData.projects().get(0);

        assertEquals(3.33d, project.baselineProductivityPerHour());
        assertEquals(17.65d, project.aiProductivityPerHour());
        assertEquals(429.41d, project.productivityGain());
        assertEquals(60.0d, project.automationCandidatePercentage());
        assertTrue(dashboardData.executiveSummary().costAvoidance() > 0.0d);
    }
}
