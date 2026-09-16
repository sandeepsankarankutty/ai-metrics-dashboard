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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Parses the input workbook into project metrics. */
public class ExcelParser {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private final DataFormatter formatter = new DataFormatter(Locale.US);

    /** Parses the workbook located at the supplied path. */
    public List<ProjectMetrics> parse(Path inputPath) {
        if (!Files.exists(inputPath)) {
            throw new ExcelParsingException("Input workbook does not exist: " + inputPath);
        }

        try (InputStream inputStream = Files.newInputStream(inputPath); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet beforeSheet = locateSheet(workbook, Constants.BEFORE_AI_SHEET_KEYWORD);
            Map<String, BaselineMetrics> baselineByProject = parseBaselineSheet(beforeSheet);
            Map<String, AIMetrics> aiByProject = parseAfterSheets(workbook, baselineByProject);
            if (aiByProject.isEmpty()) {
                throw new ExcelParsingException("Workbook is missing AI metrics sheets containing keyword: "
                        + Constants.AFTER_AI_SHEET_KEYWORD);
            }

            Map<String, ProjectMetrics> projects = new LinkedHashMap<>();
            baselineByProject.forEach((key, baseline) -> {
                AIMetrics ai = aiByProject.getOrDefault(key, emptyAiFor(baseline.projectName(), baseline.lead()));
                projects.put(key, ProjectMetrics.seed(baseline, ai));
            });
            aiByProject.forEach((key, ai) -> projects.computeIfAbsent(key,
                    missingKey -> ProjectMetrics.seed(emptyBaselineFor(ai.projectName(), ai.lead()), ai)));
            return new ArrayList<>(projects.values());
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
        Map<String, BaselineMetrics> projects = new LinkedHashMap<>();
        for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String projectName = readString(row, 0);
            if (projectName.isBlank()) {
                continue;
            }
            String lead = readString(row, 1);
            BaselineMetrics baseline = new BaselineMetrics(
                    projectName,
                    lead,
                    readDurationHours(row.getCell(2)),
                    readDurationHours(row.getCell(3)),
                    readDurationHours(row.getCell(4)),
                    readDurationHours(row.getCell(5)),
                    readDurationHours(row.getCell(6)),
                    readDurationHours(row.getCell(7)),
                    readDurationHours(row.getCell(8)),
                    readDurationHours(row.getCell(9)),
                    readNumeric(row.getCell(10)),
                    ValidationUtils.normalizePercentage(readNumeric(row.getCell(12))),
                    readNumeric(row.getCell(13)),
                    readNumeric(row.getCell(14)),
                    ValidationUtils.normalizePercentage(readNumeric(row.getCell(15))));
            projects.put(normalizeProjectKey(projectName), baseline);
        }
        return projects;
    }

    private Map<String, AIMetrics> parseAfterSheets(Workbook workbook, Map<String, BaselineMetrics> baselineByProject) {
        Map<String, AIMetrics> projects = new LinkedHashMap<>();
        for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
            Sheet sheet = workbook.getSheetAt(index);
            if (!ValidationUtils.normalizeHeader(sheet.getSheetName()).contains(Constants.AFTER_AI_SHEET_KEYWORD)) {
                continue;
            }
            if (isLegacyAfterSheet(sheet)) {
                projects.putAll(parseLegacyAfterSheet(sheet));
            } else {
                AIMetrics metrics = parseWeeklyAfterSheet(sheet, baselineByProject);
                projects.put(normalizeProjectKey(metrics.projectName()), metrics);
            }
        }
        return projects;
    }

    private boolean isLegacyAfterSheet(Sheet sheet) {
        Row firstRow = sheet.getRow(0);
        if (firstRow == null) {
            return false;
        }
        for (int col = 0; col < firstRow.getLastCellNum(); col++) {
            String value = ValidationUtils.normalizeHeader(readString(firstRow, col));
            if ("projectname".equals(value) || "project".equals(value)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, AIMetrics> parseLegacyAfterSheet(Sheet sheet) {
        Map<String, Integer> headers = readHeaders(sheet.getRow(0));
        Map<String, AIMetrics> metricsByProject = new LinkedHashMap<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            int projectCol = firstHeader(headers, "projectname");
            if (projectCol < 0) {
                projectCol = firstHeader(headers, "project");
            }
            if (row == null || readString(row, projectCol).isBlank()) {
                continue;
            }
            String projectName = readString(row, projectCol);
            String lead = readString(row, headers.getOrDefault("lead", 1));
            double stories = readByHeader(row, headers, "numberofstoriesanalyzed", "storiesanalyzed");
            double generated = readByHeader(row, headers, "numberoftestcasesgenerated", "generatedtestcases", "aitestcases");
            AIMetrics metrics = new AIMetrics(
                    projectName,
                    lead,
                    stories,
                    0.0d,
                    0.0d,
                    0.0d,
                    readByHeader(row, headers, "aitotalinteractiontime", "interactiontime"),
                    generated,
                    0.0d,
                    0.0d,
                    0.0d,
                    readByHeader(row, headers, "totaltestcasesmonth", "totaltestcasespermonth", "numberoftestcasesgenerated"),
                    ValidationUtils.normalizePercentage(readByHeader(row, headers, "testcasecoverage", "coverage")),
                    readByHeader(row, headers, "automationcandidatesidentified", "automationcandidates"),
                    readByHeader(row, headers, "reviewdefectsfoundintestcases"),
                    readByHeader(row, headers, "reviewtimepertestcase"),
                    ValidationUtils.normalizePercentage(readByHeader(row, headers, "reworkpercentage")));
            metricsByProject.put(normalizeProjectKey(projectName), metrics);
        }
        return metricsByProject;
    }

    private AIMetrics parseWeeklyAfterSheet(Sheet sheet, Map<String, BaselineMetrics> baselineByProject) {
        Row metricHeaderRow = sheet.getRow(1);
        Row complexityHeaderRow = sheet.getRow(2);
        if (metricHeaderRow == null || complexityHeaderRow == null) {
            return emptyAiFor(resolveProjectNameFromSheet(sheet.getSheetName(), baselineByProject), "");
        }
        Map<String, Integer> metricHeaders = readHeaders(metricHeaderRow);
        Map<String, Integer> complexityHeaders = readHeaders(complexityHeaderRow);

        int complexCol = firstHeader(complexityHeaders, "complex");
        int highCol = firstHeader(complexityHeaders, "high");
        int mediumCol = firstHeader(complexityHeaders, "medium");
        int lowCol = firstHeader(complexityHeaders, "low");
        int interactionCol = firstHeader(metricHeaders, "aitoolinteractiontime");
        int generatedCol = firstHeader(metricHeaders, "numberoftestcasesgeneratedusingai");
        if (generatedCol < 0) {
            generatedCol = firstHeader(metricHeaders, "numberoftestcasesgenerated");
        }
        int coverageCol = firstHeader(metricHeaders, "requirementcoverage");
        int edgeCasesCol = firstHeader(metricHeaders, "edgecasesidentified");
        int reviewDefectsCol = firstHeader(metricHeaders, "reviewdefectsfoundintestcases");
        int reworkCol = firstHeader(metricHeaders, "reworkpercentage");
        int reviewTimeCol = firstHeader(metricHeaders, "reviewtimepertestcase");
        int defectsPer100Col = firstHeader(metricHeaders, "defectsper100testcases");

        double storiesComplex = 0.0d;
        double storiesHigh = 0.0d;
        double storiesMedium = 0.0d;
        double storiesLow = 0.0d;
        double aiInteractionHours = 0.0d;
        double generatedTotal = 0.0d;
        double coverageWeightedSum = 0.0d;
        double automationCandidates = 0.0d;
        double reviewDefectsFound = 0.0d;
        double reworkWeightedSum = 0.0d;
        double reviewTimeWeightedSum = 0.0d;

        for (int rowIndex = 3; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            double generated = generatedCol >= 0 ? readNumeric(row.getCell(generatedCol)) : 0.0d;
            double rowCoverage = coverageCol >= 0 ? ValidationUtils.normalizePercentage(readNumeric(row.getCell(coverageCol))) : 0.0d;
            double rowRework = reworkCol >= 0 ? ValidationUtils.normalizePercentage(readNumeric(row.getCell(reworkCol))) : 0.0d;
            double rowReviewTime = reviewTimeCol >= 0 ? readDurationHours(row.getCell(reviewTimeCol)) : 0.0d;

            double rowStoriesComplex = complexCol >= 0 ? readNumeric(row.getCell(complexCol)) : 0.0d;
            double rowStoriesHigh = highCol >= 0 ? readNumeric(row.getCell(highCol)) : 0.0d;
            double rowStoriesMedium = mediumCol >= 0 ? readNumeric(row.getCell(mediumCol)) : 0.0d;
            double rowStoriesLow = lowCol >= 0 ? readNumeric(row.getCell(lowCol)) : 0.0d;
            if (generated == 0.0d && rowStoriesComplex == 0.0d && rowStoriesHigh == 0.0d
                    && rowStoriesMedium == 0.0d && rowStoriesLow == 0.0d) {
                continue;
            }

            storiesComplex += rowStoriesComplex;
            storiesHigh += rowStoriesHigh;
            storiesMedium += rowStoriesMedium;
            storiesLow += rowStoriesLow;
            generatedTotal += generated;
            aiInteractionHours += interactionCol >= 0 ? readDurationHours(row.getCell(interactionCol)) : 0.0d;
            automationCandidates += edgeCasesCol >= 0 ? readNumeric(row.getCell(edgeCasesCol)) : 0.0d;

            double rowReviewDefects = reviewDefectsCol >= 0 ? readNumeric(row.getCell(reviewDefectsCol)) : 0.0d;
            if (rowReviewDefects == 0.0d && defectsPer100Col >= 0 && generated > 0.0d) {
                double densityPer100 = readNumeric(row.getCell(defectsPer100Col));
                rowReviewDefects = (densityPer100 * generated) / 100.0d;
            }
            reviewDefectsFound += rowReviewDefects;

            coverageWeightedSum += rowCoverage * generated;
            reworkWeightedSum += rowRework * generated;
            reviewTimeWeightedSum += rowReviewTime * generated;
        }

        String projectName = resolveProjectNameFromSheet(sheet.getSheetName(), baselineByProject);
        String lead = "";
        BaselineMetrics baselineMetrics = baselineByProject.get(normalizeProjectKey(projectName));
        if (baselineMetrics != null) {
            lead = baselineMetrics.lead();
        }
        return new AIMetrics(
                projectName,
                lead,
                ValidationUtils.round(storiesComplex),
                ValidationUtils.round(storiesHigh),
                ValidationUtils.round(storiesMedium),
                ValidationUtils.round(storiesLow),
                ValidationUtils.round(aiInteractionHours),
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                ValidationUtils.round(generatedTotal),
                ValidationUtils.round(ValidationUtils.safeDivide(coverageWeightedSum, generatedTotal)),
                ValidationUtils.round(automationCandidates),
                ValidationUtils.round(reviewDefectsFound),
                ValidationUtils.round(ValidationUtils.safeDivide(reviewTimeWeightedSum, generatedTotal)),
                ValidationUtils.round(ValidationUtils.safeDivide(reworkWeightedSum, generatedTotal)));
    }

    private Map<String, Integer> readHeaders(Row row) {
        Map<String, Integer> headers = new LinkedHashMap<>();
        if (row == null) {
            return headers;
        }
        for (int index = 0; index < row.getLastCellNum(); index++) {
            String normalized = ValidationUtils.normalizeHeader(readString(row, index));
            if (!normalized.isBlank()) {
                headers.put(normalized, index);
            }
        }
        return headers;
    }

    private int firstHeader(Map<String, Integer> headers, String token) {
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey().contains(token))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(-1);
    }

    private double readByHeader(Row row, Map<String, Integer> headers, String... options) {
        for (String option : options) {
            int col = firstHeader(headers, option);
            if (col >= 0) {
                return readNumeric(row.getCell(col));
            }
        }
        return 0.0d;
    }

    private String readString(Row row, int col) {
        if (row == null || col < 0) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(col)).trim();
    }

    private double readNumeric(Cell cell) {
        String raw = formatter.formatCellValue(cell);
        return parseNumber(raw);
    }

    private double readDurationHours(Cell cell) {
        String raw = formatter.formatCellValue(cell);
        if (raw == null || raw.isBlank()) {
            return 0.0d;
        }
        double value = parseNumber(raw);
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (normalized.contains("min")) {
            return value / 60.0d;
        }
        return value;
    }

    private double parseNumber(String raw) {
        if (raw == null) {
            return 0.0d;
        }
        String sanitized = raw.replace(",", "").replace("%", "").trim().toLowerCase(Locale.ROOT);
        Matcher matcher = NUMBER_PATTERN.matcher(sanitized);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group());
            } catch (NumberFormatException ignored) {
                return 0.0d;
            }
        }
        return 0.0d;
    }

    private String resolveProjectNameFromSheet(String sheetName, Map<String, BaselineMetrics> baselineByProject) {
        String cleaned = ValidationUtils.normalizeHeader(sheetName);
        if (cleaned.endsWith(Constants.AFTER_AI_SHEET_KEYWORD)) {
            cleaned = cleaned.substring(0, cleaned.length() - Constants.AFTER_AI_SHEET_KEYWORD.length());
        }
        if (cleaned.isBlank()) {
            return sheetName;
        }
        if (baselineByProject.containsKey(cleaned)) {
            return baselineByProject.get(cleaned).projectName();
        }

        String bestKey = cleaned;
        int bestDistance = Integer.MAX_VALUE;
        for (String baselineKey : baselineByProject.keySet()) {
            int distance = levenshtein(cleaned, baselineKey);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestKey = baselineKey;
            }
        }
        int threshold = Math.max(2, cleaned.length() / 4);
        if (bestDistance > threshold) {
            return sheetName.replaceAll("(?i)\\s*-\\s*after\\s*ai\\s*$", "").trim();
        }
        BaselineMetrics baseline = baselineByProject.get(bestKey);
        return baseline != null ? baseline.projectName() : sheetName;
    }

    private int levenshtein(String left, String right) {
        if (left.equals(right)) {
            return 0;
        }
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int replaceCost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + replaceCost);
            }
        }
        return dp[left.length()][right.length()];
    }

    private String normalizeProjectKey(String projectName) {
        return ValidationUtils.normalizeHeader(projectName);
    }

    private AIMetrics emptyAiFor(String projectName, String lead) {
        return new AIMetrics(projectName, lead,
                0.0d, 0.0d, 0.0d, 0.0d,
                0.0d,
                0.0d, 0.0d, 0.0d, 0.0d,
                0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
    }

    private BaselineMetrics emptyBaselineFor(String projectName, String lead) {
        return new BaselineMetrics(projectName, lead,
                0.0d, 0.0d, 0.0d, 0.0d,
                0.0d, 0.0d, 0.0d, 0.0d,
                0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
    }
}
