package com.metrics.dashboard.support;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Creates repeatable workbook fixtures for tests. */
public final class TestWorkbookFactory {
    private TestWorkbookFactory() {
    }

    /** Writes a sample workbook with Before AI and After AI sheets. */
    public static Path writeSampleWorkbook(Path target) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet before = workbook.createSheet("Before AI");
            before.createRow(0).createCell(0).setCellValue("Project Name");
            before.getRow(0).createCell(1).setCellValue("Lead");
            before.getRow(0).createCell(2).setCellValue("Hours to Analyze Requirement - Complex");
            before.getRow(0).createCell(3).setCellValue("Hours to Create Test Case for Complex Requirement");
            before.getRow(0).createCell(4).setCellValue("Total test cases / month");
            before.getRow(0).createCell(5).setCellValue("% Requirement Coverage");
            before.getRow(0).createCell(6).setCellValue("Defects found from test cases");
            before.getRow(0).createCell(7).setCellValue("Review hours per 100 test cases");
            before.getRow(0).createCell(8).setCellValue("% test cases updated after review");
            writeBeforeRow(before, 1, "CenAccess", "Alex", 1.0, 4.0, 520, 96, 44, 10, 12);
            writeBeforeRow(before, 2, "Guided Flow", "Jordan", 1.2, 3.8, 410, 94, 29, 9, 10);

            Sheet after = workbook.createSheet("After AI");
            after.createRow(0).createCell(0).setCellValue("Project Name");
            after.getRow(0).createCell(1).setCellValue("Lead");
            after.getRow(0).createCell(2).setCellValue("Hours to Analyze Requirement - Complex");
            after.getRow(0).createCell(3).setCellValue("Hours to Create Test Case for Complex Requirement");
            after.getRow(0).createCell(4).setCellValue("Number of stories analyzed");
            after.getRow(0).createCell(5).setCellValue("AI Total Interaction Time");
            after.getRow(0).createCell(6).setCellValue("Total test cases / month");
            after.getRow(0).createCell(7).setCellValue("Number of test cases generated");
            after.getRow(0).createCell(8).setCellValue("Test case coverage %");
            after.getRow(0).createCell(9).setCellValue("Automation candidates identified");
            after.getRow(0).createCell(10).setCellValue("Review defects found in test cases");
            after.getRow(0).createCell(11).setCellValue("Review time per test case");
            writeAfterRow(after, 1, "CenAccess", "Alex", 0.5, 0.3, 45, 0.8, 520, 390, 96, 265, 4, 0.03);
            writeAfterRow(after, 2, "Guided Flow", "Jordan", 0.5, 0.25, 30, 0.7, 410, 280, 94, 171, 3, 0.03);

            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            try (OutputStream outputStream = Files.newOutputStream(target)) {
                workbook.write(outputStream);
            }
        }
        return target;
    }

    private static void writeBeforeRow(Sheet sheet, int rowNum, String name, String lead, double analysisHours,
            double designHours, double totalTests, double coverage, double defects, double reviewHours, double rework) {
        sheet.createRow(rowNum).createCell(0).setCellValue(name);
        sheet.getRow(rowNum).createCell(1).setCellValue(lead);
        sheet.getRow(rowNum).createCell(2).setCellValue(analysisHours);
        sheet.getRow(rowNum).createCell(3).setCellValue(designHours);
        sheet.getRow(rowNum).createCell(4).setCellValue(totalTests);
        sheet.getRow(rowNum).createCell(5).setCellValue(coverage);
        sheet.getRow(rowNum).createCell(6).setCellValue(defects);
        sheet.getRow(rowNum).createCell(7).setCellValue(reviewHours);
        sheet.getRow(rowNum).createCell(8).setCellValue(rework);
    }

    private static void writeAfterRow(Sheet sheet, int rowNum, String name, String lead, double analysisHours,
            double designHours, double stories, double interactionHours, double totalTests, double generatedTests,
            double coverage, double automationCandidates, double reviewDefects, double reviewTimePerTestCase) {
        sheet.createRow(rowNum).createCell(0).setCellValue(name);
        sheet.getRow(rowNum).createCell(1).setCellValue(lead);
        sheet.getRow(rowNum).createCell(2).setCellValue(analysisHours);
        sheet.getRow(rowNum).createCell(3).setCellValue(designHours);
        sheet.getRow(rowNum).createCell(4).setCellValue(stories);
        sheet.getRow(rowNum).createCell(5).setCellValue(interactionHours);
        sheet.getRow(rowNum).createCell(6).setCellValue(totalTests);
        sheet.getRow(rowNum).createCell(7).setCellValue(generatedTests);
        sheet.getRow(rowNum).createCell(8).setCellValue(coverage);
        sheet.getRow(rowNum).createCell(9).setCellValue(automationCandidates);
        sheet.getRow(rowNum).createCell(10).setCellValue(reviewDefects);
        sheet.getRow(rowNum).createCell(11).setCellValue(reviewTimePerTestCase);
    }
}
