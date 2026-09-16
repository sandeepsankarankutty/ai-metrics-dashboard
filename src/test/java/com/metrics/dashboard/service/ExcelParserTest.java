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
import org.apache.poi.ss.usermodel.Sheet;
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
        assertEquals(2, projects.size());
        ProjectMetrics cenAccess = projects.stream()
                .filter(project -> project.projectName().equals("CenAccess"))
                .findFirst()
                .orElseThrow();
        assertEquals(0.0d, cenAccess.aiMetrics().generatedTestCases());
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

    @Test
    void fuzzyMatchMapsNearSheetNameToBaselineProject() throws Exception {
        Path workbook = Files.createTempFile("metrics-fuzzy-near", ".xlsx");
        try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbook)) {
            Sheet before = xssfWorkbook.createSheet("Baseline - Before AI");
            before.createRow(0).createCell(0).setCellValue("Project");
            before.createRow(1).createCell(2).setCellValue("Complex");
            before.createRow(2).createCell(0).setCellValue("CenAccess");
            before.getRow(2).createCell(1).setCellValue("Alex");
            before.getRow(2).createCell(10).setCellValue(100);
            createWeeklyAfterSheet(xssfWorkbook, "CenAcces - After AI", 40);
            xssfWorkbook.write(outputStream);
        }

        List<ProjectMetrics> projects = new ExcelParser().parse(workbook);
        assertEquals(1, projects.size());
        assertEquals(40.0d, projects.get(0).aiMetrics().generatedTestCases());
    }

    @Test
    void fuzzyMatchDoesNotMergeDistantSheetName() throws Exception {
        Path workbook = Files.createTempFile("metrics-fuzzy-far", ".xlsx");
        try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbook)) {
            Sheet before = xssfWorkbook.createSheet("Baseline - Before AI");
            before.createRow(0).createCell(0).setCellValue("Project");
            before.createRow(1).createCell(2).setCellValue("Complex");
            before.createRow(2).createCell(0).setCellValue("CenAccess");
            before.getRow(2).createCell(1).setCellValue("Alex");
            before.getRow(2).createCell(10).setCellValue(100);
            createWeeklyAfterSheet(xssfWorkbook, "Completely Different - After AI", 40);
            xssfWorkbook.write(outputStream);
        }

        List<ProjectMetrics> projects = new ExcelParser().parse(workbook);
        assertEquals(2, projects.size());
        ProjectMetrics cenAccess = projects.stream()
                .filter(project -> project.projectName().equals("CenAccess"))
                .findFirst()
                .orElseThrow();
        assertEquals(0.0d, cenAccess.aiMetrics().generatedTestCases());
        assertTrue(projects.stream().anyMatch(project -> project.projectName().equals("Completely Different")
                && project.aiMetrics().generatedTestCases() == 40.0d));
    }

    @Test
    void parsesMultipleProjectsFromLegacyAfterAiSheet() throws Exception {
        Path workbook = Files.createTempFile("metrics-legacy-after", ".xlsx");
        try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbook)) {
            Sheet before = xssfWorkbook.createSheet("Before AI");
            before.createRow(0).createCell(0).setCellValue("Project");
            before.createRow(1).createCell(2).setCellValue("Complex");
            before.createRow(2).createCell(0).setCellValue("CenAccess");
            before.getRow(2).createCell(10).setCellValue(100);
            before.createRow(3).createCell(0).setCellValue("Guided Flow");
            before.getRow(3).createCell(10).setCellValue(90);

            Sheet after = xssfWorkbook.createSheet("After AI");
            after.createRow(0).createCell(0).setCellValue("Project Name");
            after.getRow(0).createCell(1).setCellValue("Lead");
            after.getRow(0).createCell(2).setCellValue("Number of stories analyzed");
            after.getRow(0).createCell(3).setCellValue("AI Total Interaction Time");
            after.getRow(0).createCell(4).setCellValue("Number of test cases generated");
            after.getRow(0).createCell(5).setCellValue("Requirement Coverage %");
            after.createRow(1).createCell(0).setCellValue("CenAccess");
            after.getRow(1).createCell(4).setCellValue(40);
            after.createRow(2).createCell(0).setCellValue("Guided Flow");
            after.getRow(2).createCell(4).setCellValue(30);
            xssfWorkbook.write(outputStream);
        }

        List<ProjectMetrics> projects = new ExcelParser().parse(workbook);
        assertEquals(2, projects.size());
        assertTrue(projects.stream().anyMatch(project -> project.projectName().equals("CenAccess")
                && project.aiMetrics().generatedTestCases() == 40.0d));
        assertTrue(projects.stream().anyMatch(project -> project.projectName().equals("Guided Flow")
                && project.aiMetrics().generatedTestCases() == 30.0d));
    }

    private void createWeeklyAfterSheet(XSSFWorkbook workbook, String sheetName, double generated) {
        Sheet after = workbook.createSheet(sheetName);
        after.createRow(0).createCell(1).setCellValue("Requirements");
        after.createRow(1).createCell(6).setCellValue("AI Tool Interaction Time");
        after.getRow(1).createCell(7).setCellValue("Number of test cases generated using AI");
        after.getRow(1).createCell(8).setCellValue("Requirement Coverage %");
        after.getRow(1).createCell(11).setCellValue("Edge Cases Identified");
        after.getRow(1).createCell(12).setCellValue("Review Defects Found in Test Cases");
        after.getRow(1).createCell(13).setCellValue("Rework Percentage");
        after.getRow(1).createCell(14).setCellValue("Review Time per Test Case");
        after.getRow(1).createCell(16).setCellValue("Defects per 100 Test Cases");
        after.createRow(2).createCell(1).setCellValue("Complex or 8+ SP");
        after.getRow(2).createCell(2).setCellValue("High or 5 SP");
        after.getRow(2).createCell(3).setCellValue("Medium or 2.3 SP");
        after.getRow(2).createCell(4).setCellValue("Low or 0.1 SP");
        after.createRow(3).createCell(1).setCellValue(3);
        after.getRow(3).createCell(6).setCellValue("10 min");
        after.getRow(3).createCell(7).setCellValue(generated);
        after.getRow(3).createCell(8).setCellValue(95);
    }
}
