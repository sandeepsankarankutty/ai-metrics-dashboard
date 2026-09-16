package com.metrics.dashboard.model;

import java.util.List;
import java.util.Map;

/** Root dashboard view model for template rendering. */
public record DashboardData(
        BaselineSummary baselineSummary,
        AIProductivitySummary aiProductivitySummary,
        ExecutiveSummary executiveSummary,
        GovernanceSummary governanceSummary,
        List<KPIData> kpis,
        List<ProjectMetrics> projects,
        Map<String, Object> charts,
        String generatedAt) {
}
