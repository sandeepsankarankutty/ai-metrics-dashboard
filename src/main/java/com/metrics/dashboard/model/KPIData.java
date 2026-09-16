package com.metrics.dashboard.model;

/** Portfolio KPI value, target, and status. */
public record KPIData(
        String name,
        double currentValue,
        double target,
        KPIStatus status,
        String statusClass,
        String description,
        String unit) {
}
