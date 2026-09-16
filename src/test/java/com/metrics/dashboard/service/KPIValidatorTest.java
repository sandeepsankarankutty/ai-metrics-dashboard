package com.metrics.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.metrics.dashboard.model.KPIStatus;
import org.junit.jupiter.api.Test;

class KPIValidatorTest {
    private final KPIValidator validator = new KPIValidator();

    @Test
    void returnsGreenWhenValueMeetsTarget() {
        assertEquals(KPIStatus.GREEN, validator.determineStatus(30.0d, 30.0d));
    }

    @Test
    void returnsYellowNearTarget() {
        assertEquals(KPIStatus.YELLOW, validator.determineStatus(24.0d, 30.0d));
    }

    @Test
    void returnsRedBelowSeventyFivePercentOfTarget() {
        assertEquals(KPIStatus.RED, validator.determineStatus(20.0d, 30.0d));
    }
}
