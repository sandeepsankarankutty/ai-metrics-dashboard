package com.metrics.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.metrics.dashboard.support.TestWorkbookFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class DashboardGeneratorTest {
    @Test
    void generatesDashboardHtmlFromWorkbook() throws Exception {
        Path workbook = TestWorkbookFactory.writeSampleWorkbook(Files.createTempFile("metrics", ".xlsx"));
        Path output = Files.createTempFile("dashboard", ".html");

        int exitCode = new CommandLine(new DashboardGenerator()).execute(
                "--input", workbook.toString(),
                "--output", output.toString());

        String html = Files.readString(output);
        assertEquals(0, exitCode);
        assertTrue(html.contains("AI Productivity Dashboard"));
        assertTrue(html.contains("CenAccess"));
        assertTrue(html.contains("Executive Summary") || html.contains("Total Stories"));
    }
}
