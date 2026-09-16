package com.metrics.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    }

    @Test
    void failsWhenRequiredHeaderIsMissing() throws Exception {
        Path workbook = Files.createTempFile("metrics-missing-header", ".xlsx");
        try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbook)) {
            xssfWorkbook.createSheet("Before AI").createRow(0).createCell(0).setCellValue("Project Name");
            xssfWorkbook.createSheet("After AI").createRow(0).createCell(0).setCellValue("Project Name");
            xssfWorkbook.write(outputStream);
        }

        assertThrows(ExcelParsingException.class, () -> new ExcelParser().parse(workbook));
    }

    @Test
    void failsWhenProjectExistsInOnlyOneSheet() throws Exception {
        Path workbook = Files.createTempFile("metrics-mismatch", ".xlsx");
        try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbook)) {
            var before = xssfWorkbook.createSheet("Before AI");
            before.createRow(0).createCell(0).setCellValue("Project Name");
            before.getRow(0).createCell(1).setCellValue("Lead");
            before.createRow(1).createCell(0).setCellValue("CenAccess");
            before.getRow(1).createCell(1).setCellValue("Alex");

            var after = xssfWorkbook.createSheet("After AI");
            after.createRow(0).createCell(0).setCellValue("Project Name");
            after.getRow(0).createCell(1).setCellValue("Lead");
            after.createRow(1).createCell(0).setCellValue("Guided Flow");
            after.getRow(1).createCell(1).setCellValue("Jordan");
            xssfWorkbook.write(outputStream);
        }

        assertThrows(ExcelParsingException.class, () -> new ExcelParser().parse(workbook));
    }

    @Test
    void matchesProjectsCaseInsensitivelyAcrossSheets() throws Exception {
        Path workbook = Files.createTempFile("metrics-case", ".xlsx");
        try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook(); OutputStream outputStream = Files.newOutputStream(workbook)) {
            var before = xssfWorkbook.createSheet("Before AI");
            before.createRow(0).createCell(0).setCellValue("Project Name");
            before.getRow(0).createCell(1).setCellValue("Lead");
            before.getRow(0).createCell(2).setCellValue("Total test cases / month");
            before.createRow(1).createCell(0).setCellValue("CenAccess");
            before.getRow(1).createCell(1).setCellValue("Alex");
            before.getRow(1).createCell(2).setCellValue(30);

            var after = xssfWorkbook.createSheet("After AI");
            after.createRow(0).createCell(0).setCellValue("Project Name");
            after.getRow(0).createCell(1).setCellValue("Lead");
            after.getRow(0).createCell(2).setCellValue("Number of test cases generated");
            after.createRow(1).createCell(0).setCellValue("cenaccess");
            after.getRow(1).createCell(1).setCellValue("Alex");
            after.getRow(1).createCell(2).setCellValue(25);
            xssfWorkbook.write(outputStream);
        }

        List<ProjectMetrics> projects = new ExcelParser().parse(workbook);

        assertEquals(1, projects.size());
        assertEquals("CenAccess", projects.get(0).projectName());
        assertEquals(25.0d, projects.get(0).aiMetrics().generatedTestCases());
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
}
