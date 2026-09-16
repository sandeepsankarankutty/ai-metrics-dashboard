package com.metrics.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.metrics.dashboard.model.ProjectMetrics;
import com.metrics.dashboard.support.TestWorkbookFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExcelParserTest {
    @Test
    void parsesProjectsFromWorkbook() throws Exception {
        Path workbook = TestWorkbookFactory.writeSampleWorkbook(Files.createTempFile("metrics", ".xlsx"));

        List<ProjectMetrics> projects = new ExcelParser().parse(workbook);

        assertEquals(2, projects.size());
        assertEquals("CenAccess", projects.get(0).projectName());
        assertEquals(520.0d, projects.get(0).baselineMetrics().totalTestCasesPerMonth());
        assertEquals(390.0d, projects.get(0).aiMetrics().generatedTestCases());
    }
}
