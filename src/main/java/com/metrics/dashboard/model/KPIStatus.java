package com.metrics.dashboard.model;

/** Color-coded KPI status indicator. */
public enum KPIStatus {
    GREEN("green"),
    YELLOW("yellow"),
    RED("red");

    private final String cssClass;

    KPIStatus(String cssClass) {
        this.cssClass = cssClass;
    }

    public String getCssClass() {
        return cssClass;
    }
}
