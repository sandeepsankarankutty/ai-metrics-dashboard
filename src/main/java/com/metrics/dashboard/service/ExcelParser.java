package com.metrics.dashboard.service;

import com.metrics.dashboard.exception.ExcelParsingException;
import com.metrics.dashboard.model.AIMetrics;
import com.metrics.dashboard.model.BaselineMetrics;
import com.metrics.dashboard.model.ProjectMetrics;
import com.metrics.dashboard.util.Constants;
import com.metrics.dashboard.util.ValidationUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Parses the input workbook into project metrics. */
public class ExcelParser {
    private final DataFormatter formatter = new DataFormatter(Locale.US);

    /** Parses the workbook located at the supplied path. */
    public List<ProjectMetrics> parse(Path inputPath) {
        if (!Files.exists(inputPath)) {
            throw new ExcelParsingException("Input workbook does not exist: " + inputPath);
        }

        try (InputStream inputStream = Files.newInputStream(inputPath); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet beforeSheet = locateSheet(workbook, Constants.BEFORE_AI_SHEET_KEYWORD);
            Sheet afterSheet = locateSheet(workbook, Constants.AFTER_AI_SHEET_KEYWORD);

            Map<String, BaselineMetrics> baselineByProject = parseBaselineSheet(beforeSheet);
            Map<String, AIMetrics> aiByProject = parseAiSheet(afterSheet);
            Set<String> projectNames = new TreeSet<>();
            projectNames.addAll(baselineByProject.keySet());
            projectNames.addAll(aiByProject.keySet());

            List<ProjectMetrics> projects = new ArrayList<>();
            for (String projectName : projectNames) {
                BaselineMetrics baseline = baselineByProject.get(projectName);
                AIMetrics ai = aiByProject.get(projectName);
                if (baseline == null || ai == null) {
                    throw new ExcelParsingException("Project '" + projectName + "' must exist in both Before AI and After AI sheets.");
                }
                projects.add(ProjectMetrics.seed(baseline, ai));
            }
            return projects;
        } catch (IOException exception) {
            throw new ExcelParsingException("Failed to parse workbook: " + inputPath, exception);
        }
    }

    private Sheet locateSheet(Workbook workbook, String keyword) {
        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            Sheet sheet = workbook.getSheetAt(index);
            if (ValidationUtils.normalizeHeader(sheet.getSheetName()).contains(keyword)) {
                return sheet;
            }
        }
        throw new ExcelParsingException("Workbook is missing a sheet containing keyword: " + keyword);
    }

    private Map<String, BaselineMetrics> parseBaselineSheet(Sheet sheet) {
        Map<String, Integer> headers = readHeaders(sheet);
        validateRequiredHeaders(headers, sheet.getSheetName());
        Map<String, BaselineMetrics> projects = new LinkedHashMap<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isRowBlank(row, headers.size())) {
                continue;
            }
            String projectName = readString(row, headers, "projectname");
            String lead = readString(row, headers, "lead");
            Map<String, Double> numericValues = extractNumericValues(row, headers);
            BaselineMetrics metrics = new BaselineMetrics(
                    projectName,
                    lead,
                    ValidationUtils.round(ValidationUtils.sumMatching(numericValues, "hours", "analyze")),
                    ValidationUtils.round(ValidationUtils.sumMatching(numericValues, "hours", "createtestcase")
                            + ValidationUtils.sumMatching(numericValues, "hours", "designtest")),
                    ValidationUtils.round(firstMatching(numericValues, "totaltestcasesmonth", "totaltestcasespermonth")),
                    ValidationUtils.round(ValidationUtils.normalizePercentage(firstMatching(numericValues, "requirementcoverage"))),
                    ValidationUtils.round(firstMatching(numericValues, "defectsfoundfromtestcases")),
                    ValidationUtils.round(firstMatching(numericValues, "reviewhoursper100testcases", "reviewhoursper100cases")),
                    ValidationUtils.round(ValidationUtils.normalizePercentage(firstMatching(numericValues, "testcasesupdatedafterreview", "percenttestcasesupdatedafterreview", "reworkrate"))));
            projects.put(normalizeProjectKey(projectName), metrics);
        }
        return projects;
    }

    private Map<String, AIMetrics> parseAiSheet(Sheet sheet) {
        Map<String, Integer> headers = readHeaders(sheet);
        validateRequiredHeaders(headers, sheet.getSheetName());
        Map<String, AIMetrics> projects = new LinkedHashMap<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isRowBlank(row, headers.size())) {
                continue;
            }
            String projectName = readString(row, headers, "projectname");
            String lead = readString(row, headers, "lead");
            Map<String, Double> numericValues = extractNumericValues(row, headers);
            AIMetrics metrics = new AIMetrics(
                    projectName,
                    lead,
                    ValidationUtils.round(ValidationUtils.sumMatching(numericValues, "hours", "analyze")),
                    ValidationUtils.round(ValidationUtils.sumMatching(numericValues, "hours", "createtestcase")
                            + ValidationUtils.sumMatching(numericValues, "hours", "designtest")),
                    ValidationUtils.round(firstMatching(numericValues, "numberofstoriesanalyzed", "storiesanalyzed")),
                    ValidationUtils.round(firstMatching(numericValues, "aitotalinteractiontime", "interactiontime")),
                    ValidationUtils.round(firstMatching(numericValues, "totaltestcasesmonth", "totaltestcasespermonth")),
                    ValidationUtils.round(firstMatching(numericValues, "numberoftestcasesgenerated", "generatedtestcases", "aitestcases")),
                    ValidationUtils.round(ValidationUtils.normalizePercentage(firstMatching(numericValues, "testcasecoverage", "testcasecoveragepct"))),
                    ValidationUtils.round(firstMatching(numericValues, "automationcandidatesidentified", "automationcandidates", "automationcandidatecount")),
                    ValidationUtils.round(firstMatching(numericValues, "reviewdefectsfoundintestcases")),
                    ValidationUtils.round(firstMatching(numericValues, "reviewtimepertestcase")));
            projects.put(normalizeProjectKey(projectName), metrics);
        }
        return projects;
    }

    private Map<String, Integer> readHeaders(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new ExcelParsingException("Sheet '" + sheet.getSheetName() + "' is missing a header row.");
        }
        Map<String, Integer> headers = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            headers.put(ValidationUtils.normalizeHeader(formatter.formatCellValue(cell)), cell.getColumnIndex());
        }
        return headers;
    }

    private void validateRequiredHeaders(Map<String, Integer> headers, String sheetName) {
        for (String required : Constants.REQUIRED_PROJECT_HEADERS) {
            if (!headers.containsKey(required)) {
                throw new ExcelParsingException("Sheet '" + sheetName + "' is missing required header: " + required);
            }
        }
    }

    private Map<String, Double> extractNumericValues(Row row, Map<String, Integer> headers) {
        Map<String, Double> values = new LinkedHashMap<>();
        headers.forEach((header, index) -> values.put(header, readNumeric(row.getCell(index))));
        return values;
    }

    private double firstMatching(Map<String, Double> values, String... candidates) {
        for (String candidate : candidates) {
            if (values.containsKey(candidate)) {
                return values.get(candidate);
            }
        }
        return 0.0d;
    }

    private String readString(Row row, Map<String, Integer> headers, String headerKey) {
        Integer index = headers.get(headerKey);
        if (index == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(index)).trim();
    }

    private double readNumeric(Cell cell) {
        if (cell == null) {
            return 0.0d;
        }
        CellType cellType = cell.getCellType();
        if (cellType == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        if (cellType == CellType.FORMULA) {
            try {
                return cell.getNumericCellValue();
            } catch (IllegalStateException ignored) {
                return parseNumericString(formatter.formatCellValue(cell));
            }
        }
        return parseNumericString(formatter.formatCellValue(cell));
    }

    private double parseNumericString(String raw) {
        String sanitized = raw == null ? "" : raw.trim().replace("%", "").replace(",", "");
        if (sanitized.isEmpty()) {
            return 0.0d;
        }
        try {
            return Double.parseDouble(sanitized);
        } catch (NumberFormatException ignored) {
            return 0.0d;
        }
    }

    private boolean isRowBlank(Row row, int width) {
        for (int index = 0; index < width; index++) {
            Cell cell = row.getCell(index);
            if (cell != null && !formatter.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String normalizeProjectKey(String projectName) {
        return projectName == null ? "" : projectName.trim().toLowerCase(Locale.ROOT);
    }
}
