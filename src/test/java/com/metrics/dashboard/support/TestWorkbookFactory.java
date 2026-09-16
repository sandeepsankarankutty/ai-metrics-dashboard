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
            Sheet before = workbook.createSheet("Baseline - Before AI");
            before.createRow(0).createCell(0).setCellValue("Project");
            before.getRow(0).createCell(1).setCellValue("Lead");
            before.createRow(1).createCell(2).setCellValue("Complex or 8+ SP");
            before.getRow(1).createCell(3).setCellValue("High or 5 SP");
            before.getRow(1).createCell(4).setCellValue("Medium or 2.3 SP");
            before.getRow(1).createCell(5).setCellValue("Low or 0.1 SP");
            before.getRow(1).createCell(6).setCellValue("Hours to Create test case for Complex");
            before.getRow(1).createCell(7).setCellValue("Hours to Create test case for High");
            before.getRow(1).createCell(8).setCellValue("Hours to Create test case for Medium");
            before.getRow(1).createCell(9).setCellValue("Hours to Create test case for Low");
            before.getRow(1).createCell(10).setCellValue("Total test cases / month");
            before.getRow(1).createCell(12).setCellValue("% Requirement Coverage");
            before.getRow(1).createCell(13).setCellValue("Defects found from created tests");
            before.getRow(1).createCell(14).setCellValue("Review hours per 100 test cases");
            before.getRow(1).createCell(15).setCellValue("% test cases updated after review");
            writeBeforeRow(before, 2, "CenAccess", "Alex", 1.0, 2.0, 1.0, 0.5, 4.0, 3.0, 2.0, 1.0, 520, 96, 44, 10, 12);
            writeBeforeRow(before, 3, "Guided Flow", "Jordan", 1.2, 1.8, 1.0, 0.5, 3.8, 2.5, 1.2, 0.8, 410, 94, 29, 9, 10);

            Sheet afterCenAccess = workbook.createSheet("CenAccess - After AI");
            writeAfterHeaders(afterCenAccess, 6, 7, 8, 11, 12, 13, 14, 15, 16);
            writeAfterRow(afterCenAccess, 3, 10, 8, 6, 4, "48 min", 390, 96, "Yes - 265", "4", "2 mins", 3, 5, 0.8);

            Sheet afterGuidedFlow = workbook.createSheet("Guided Flow - After AI");
            writeAfterHeaders(afterGuidedFlow, 5, 6, 7, 10, 11, 12, 13, 14, 15);
            writeAfterRow(afterGuidedFlow, 3, 8, 5, 4, 3, "42 min", 280, 94, "Yes - 171", "3", "2 mins", 2, 3, 0.9);

            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            try (OutputStream outputStream = Files.newOutputStream(target)) {
                workbook.write(outputStream);
            }
        }
        return target;
    }

    private static void writeBeforeRow(Sheet sheet, int rowNum, String name, String lead, double analysisComplex,
            double analysisHigh, double analysisMedium, double analysisLow,
            double designComplex, double designHigh, double designMedium, double designLow,
            double totalTests, double coverage, double defects, double reviewHours, double rework) {
        sheet.createRow(rowNum).createCell(0).setCellValue(name);
        sheet.getRow(rowNum).createCell(1).setCellValue(lead);
        sheet.getRow(rowNum).createCell(2).setCellValue(analysisComplex);
        sheet.getRow(rowNum).createCell(3).setCellValue(analysisHigh);
        sheet.getRow(rowNum).createCell(4).setCellValue(analysisMedium);
        sheet.getRow(rowNum).createCell(5).setCellValue(analysisLow);
        sheet.getRow(rowNum).createCell(6).setCellValue(designComplex);
        sheet.getRow(rowNum).createCell(7).setCellValue(designHigh);
        sheet.getRow(rowNum).createCell(8).setCellValue(designMedium);
        sheet.getRow(rowNum).createCell(9).setCellValue(designLow);
        sheet.getRow(rowNum).createCell(10).setCellValue(totalTests);
        sheet.getRow(rowNum).createCell(12).setCellValue(coverage);
        sheet.getRow(rowNum).createCell(13).setCellValue(defects);
        sheet.getRow(rowNum).createCell(14).setCellValue(reviewHours);
        sheet.getRow(rowNum).createCell(15).setCellValue(rework);
    }

    private static void writeAfterHeaders(Sheet sheet, int interactionCol, int generatedCol, int coverageCol,
            int edgeCasesCol, int reviewDefectsCol, int reworkCol, int reviewTimeCol, int rejectedCol, int defectsPer100Col) {
        sheet.createRow(0).createCell(1).setCellValue("Requirements");
        sheet.getRow(0).createCell(generatedCol).setCellValue("Test Cases");
        sheet.createRow(1).createCell(0).setCellValue("Week");
        sheet.getRow(1).createCell(interactionCol).setCellValue("AI Tool Interaction Time");
        sheet.getRow(1).createCell(generatedCol).setCellValue("Number of test cases generated using AI");
        sheet.getRow(1).createCell(coverageCol).setCellValue("Requirement Coverage %");
        sheet.getRow(1).createCell(edgeCasesCol).setCellValue("Edge Cases Identified");
        sheet.getRow(1).createCell(reviewDefectsCol).setCellValue("Review Defects Found in Test Cases");
        sheet.getRow(1).createCell(reworkCol).setCellValue("Rework Percentage");
        sheet.getRow(1).createCell(reviewTimeCol).setCellValue("Review Time per Test Case");
        sheet.getRow(1).createCell(rejectedCol).setCellValue("Test Cases Rejected");
        sheet.getRow(1).createCell(defectsPer100Col).setCellValue("Defects per 100 Test Cases");
        sheet.createRow(2).createCell(1).setCellValue("Complex or 8+ SP");
        sheet.getRow(2).createCell(2).setCellValue("High or 5 SP");
        sheet.getRow(2).createCell(3).setCellValue("Medium or 2.3 SP");
        sheet.getRow(2).createCell(4).setCellValue("Low or 0.1 SP");
    }

    private static void writeAfterRow(Sheet sheet, int rowNum, double storiesComplex, double storiesHigh, double storiesMedium,
            double storiesLow, String interaction, double generatedTests, double coverage, String edgeCases, String reviewDefects,
            String reviewTimePerTestCase, double rejected, double modified, double defectsPer100) {
        sheet.createRow(rowNum).createCell(0).setCellValue("07 Sep - 11 Sep");
        sheet.getRow(rowNum).createCell(1).setCellValue(storiesComplex);
        sheet.getRow(rowNum).createCell(2).setCellValue(storiesHigh);
        sheet.getRow(rowNum).createCell(3).setCellValue(storiesMedium);
        sheet.getRow(rowNum).createCell(4).setCellValue(storiesLow);
        for (int col = 5; col < sheet.getRow(1).getLastCellNum(); col++) {
            String header = sheet.getRow(1).getCell(col) == null ? "" : sheet.getRow(1).getCell(col).getStringCellValue();
            if (header.contains("Interaction")) {
                sheet.getRow(rowNum).createCell(col).setCellValue(interaction);
            } else if (header.contains("generated")) {
                sheet.getRow(rowNum).createCell(col).setCellValue(generatedTests);
            } else if (header.contains("Coverage")) {
                sheet.getRow(rowNum).createCell(col).setCellValue(coverage);
            } else if (header.contains("Edge Cases")) {
                sheet.getRow(rowNum).createCell(col).setCellValue(edgeCases);
            } else if (header.contains("Review Defects")) {
                sheet.getRow(rowNum).createCell(col).setCellValue(reviewDefects);
            } else if (header.contains("Rework")) {
                sheet.getRow(rowNum).createCell(col).setCellValue(0.03);
            } else if (header.contains("Review Time")) {
                sheet.getRow(rowNum).createCell(col).setCellValue(reviewTimePerTestCase);
            } else if (header.contains("Rejected")) {
                sheet.getRow(rowNum).createCell(col).setCellValue(rejected);
            } else if (header.contains("Modified")) {
                sheet.getRow(rowNum).createCell(col).setCellValue(modified);
            } else if (header.contains("Defects per 100")) {
                sheet.getRow(rowNum).createCell(col).setCellValue(defectsPer100);
            }
        }
    }
}
