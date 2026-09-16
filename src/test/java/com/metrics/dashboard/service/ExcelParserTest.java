package com.metrics.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.metrics.dashboard.exception.ExcelParsingException;
import com.metrics.dashboard.model.ProjectMetrics;
import com.metrics.dashboard.support.TestWorkbookFactory;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
        assertEquals(4.0d, projects.get(0).aiMetrics().reviewDefectsFound());
    }

    @Test
    void failsWhenBeforeAiSheetIsMissing() throws Exception {
        Path workbook = Files.createTempFile("metrics-missing-header", ".xlsx");
        try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbook)) {
            xssfWorkbook.createSheet("Baseline").createRow(0).createCell(0).setCellValue("Project");
            xssfWorkbook.createSheet("CenAccess - After AI");
            xssfWorkbook.write(outputStream);
        }

        assertThrows(ExcelParsingException.class, () -> new ExcelParser().parse(workbook));
    }

    @Test
    void defaultsAiMetricsWhenProjectExistsOnlyInBeforeSheet() throws Exception {
        Path workbook = Files.createTempFile("metrics-mismatch", ".xlsx");
        try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbook)) {
            var before = xssfWorkbook.createSheet("Baseline - Before AI");
            before.createRow(0).createCell(0).setCellValue("Project");
            before.getRow(0).createCell(1).setCellValue("Lead");
            before.createRow(1).createCell(2).setCellValue("Complex");
            before.createRow(2).createCell(0).setCellValue("CenAccess");
            before.getRow(2).createCell(1).setCellValue("Alex");
            before.getRow(2).createCell(10).setCellValue(120);
            xssfWorkbook.createSheet("After AI");
            xssfWorkbook.write(outputStream);
        }

        List<ProjectMetrics> projects = new ExcelParser().parse(workbook);
        assertEquals(1, projects.size());
        assertEquals(0.0d, projects.get(0).aiMetrics().generatedTestCases());
    }

    @Test
    void failsWhenExpectedSheetNamesAreMissing() throws Exception {
        Path workbook = Files.createTempFile("metrics-sheetnames", ".xlsx");
        try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbook)) {
            xssfWorkbook.createSheet("Manual Baseline").createRow(0).createCell(0).setCellValue("Project Name");
            xssfWorkbook.getSheetAt(0).getRow(0).createCell(1).setCellValue("Lead");
            xssfWorkbook.createSheet("AI Assisted").createRow(0).createCell(0).setCellValue("Project Name");
            xssfWorkbook.getSheetAt(1).getRow(0).createCell(1).setCellValue("Lead");
            xssfWorkbook.write(outputStream);
        }

        assertThrows(ExcelParsingException.class, () -> new ExcelParser().parse(workbook));
    }

    @Test
    void parsesActualQaEffortWorkbook() {
        Path workbook = Path.of("sample_data", "QA_Effort_Spent.xlsx");
        List<ProjectMetrics> projects = new ExcelParser().parse(workbook);

        assertTrue(projects.size() >= 8);
        ProjectMetrics digital = projects.stream()
                .filter(project -> project.projectName().equalsIgnoreCase("Digital Initiatives"))
                .findFirst()
                .orElseThrow();
        assertTrue(digital.aiMetrics().generatedTestCases() > 100.0d);
        assertTrue(digital.aiMetrics().automationCandidates() > 0.0d);
    }
}
