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
        BaselineMetrics baseline = new BaselineMetrics(
                "CenAccess", "Alex",
                1.0d, 1.0d, 1.0d, 1.0d,
                2.0d, 2.0d, 2.0d, 2.0d,
                120.0d, 96.0d, 12.0d, 5.0d, 25.0d);
        AIMetrics ai = new AIMetrics(
                "CenAccess", "Alex",
                10.0d, 5.0d, 0.0d, 0.0d,
                4.0d,
                0.0d, 0.0d, 0.0d, 0.0d,
                60.0d, 97.0d, 30.0d, 3.0d, 0.05d, 3.0d);

        DashboardData dashboardData = new MetricsCalculator().buildDashboardData(List.of(ProjectMetrics.seed(baseline, ai)));
        ProjectMetrics project = dashboardData.projects().get(0);

        assertEquals(10.0d, project.baselineProductivityPerHour());
        assertEquals(8.57d, project.aiProductivityPerHour());
        assertEquals(-14.29d, project.productivityGain());
        assertEquals(50.0d, project.automationCandidatePercentage());
        assertEquals(50.0d, project.qualityImprovement());
        assertTrue(dashboardData.executiveSummary().totalCostAvoidance() >= 0.0d);
    }
}
