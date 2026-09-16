package com.metrics.dashboard;

import com.metrics.dashboard.exception.DashboardException;
import com.metrics.dashboard.model.DashboardData;
import com.metrics.dashboard.model.ProjectMetrics;
import com.metrics.dashboard.service.ExcelParser;
import com.metrics.dashboard.service.HTMLGenerator;
import com.metrics.dashboard.service.MetricsCalculator;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Main CLI entry point for generating the dashboard. */
@Command(name = "dashboard-generator", mixinStandardHelpOptions = true,
        description = "Generates an interactive AI metrics dashboard from an Excel workbook.")
public class DashboardGenerator implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardGenerator.class);

    @Option(names = {"-i", "--input"}, required = true, description = "Path to the input Excel workbook")
    private Path inputPath;

    @Option(names = {"-o", "--output"}, required = true, description = "Path to the output HTML file")
    private Path outputPath;

    @Option(names = {"-v", "--verbose"}, description = "Enable debug logging")
    private boolean verbose;

    private final ExcelParser excelParser;
    private final MetricsCalculator metricsCalculator;
    private final HTMLGenerator htmlGenerator;

    public DashboardGenerator() {
        this(new ExcelParser(), new MetricsCalculator(), new HTMLGenerator());
    }

    public DashboardGenerator(ExcelParser excelParser, MetricsCalculator metricsCalculator, HTMLGenerator htmlGenerator) {
        this.excelParser = excelParser;
        this.metricsCalculator = metricsCalculator;
        this.htmlGenerator = htmlGenerator;
    }

    /** Application main method. */
    public static void main(String[] args) {
        if (containsVerboseFlag(args)) {
            System.setProperty("LOG_LEVEL", "DEBUG");
        }
        int exitCode = new CommandLine(new DashboardGenerator()).execute(args);
        System.exit(exitCode);
    }

    /** Executes the parse-calculate-render workflow. */
    @Override
    public Integer call() {
        try {
            LOGGER.info("Generating dashboard from {} to {}", inputPath, outputPath);
            if (verbose) {
                LOGGER.debug("Verbose logging enabled");
            }
            List<ProjectMetrics> parsedProjects = excelParser.parse(inputPath);
            DashboardData dashboardData = metricsCalculator.buildDashboardData(parsedProjects);
            htmlGenerator.generate(dashboardData, outputPath);
            LOGGER.info("Dashboard generated successfully: {}", outputPath);
            return 0;
        } catch (DashboardException exception) {
            LOGGER.error("Dashboard generation failed: {}", exception.getMessage(), exception);
            return 1;
        }
    }

    private static boolean containsVerboseFlag(String[] args) {
        for (String arg : args) {
            if ("-v".equals(arg) || "--verbose".equals(arg)) {
                return true;
            }
        }
        return false;
    }
}
