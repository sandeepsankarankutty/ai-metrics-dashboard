package com.metrics.dashboard.util;

import java.util.List;

/** Shared application constants. */
public final class Constants {
    public static final String BEFORE_AI_SHEET_KEYWORD = "beforeai";
    public static final String AFTER_AI_SHEET_KEYWORD = "afterai";
    public static final double QA_HOURLY_RATE = 75.0d;
    public static final double HOURS_PER_QA_DAY = 8.0d;
    public static final double PRODUCTIVITY_GAIN_TARGET = 30.0d;
    public static final double AI_ADOPTION_TARGET = 60.0d;
    public static final double REVIEW_REDUCTION_TARGET = 25.0d;
    public static final double REQUIREMENT_COVERAGE_TARGET = 95.0d;
    public static final double AUTOMATION_READINESS_TARGET = 50.0d;
    public static final List<String> REQUIRED_PROJECT_HEADERS = List.of("projectname", "lead");

    private Constants() {
    }
}
