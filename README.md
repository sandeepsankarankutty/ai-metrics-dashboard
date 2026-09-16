# ai-metrics-dashboard

Java CLI for generating an interactive HTML dashboard from Excel-based QA and AI testing metrics.

## Build

```bash
mvn clean package
```

## Run

```bash
java -jar target/ai-metrics-dashboard-1.0.0-SNAPSHOT.jar --input sample_data/sample_metrics.xlsx --output dashboard.html
```

## Options

- `-i`, `--input` - input Excel workbook (`.xlsx`)
- `-o`, `--output` - output HTML file
- `-v`, `--verbose` - enable debug logging
- `-h`, `--help` - show help

## Workbook format

Use two sheets named `Before AI` and `After AI` (or sheet names containing those phrases). Each row represents one application/project.

The parser supports the metrics described in the issue, including requirement-analysis hours, test-case creation hours, coverage, review effort, AI interaction time, stories analyzed, tests generated, and automation candidates.

A sample workbook is included at `/home/runner/work/ai-metrics-dashboard/ai-metrics-dashboard/sample_data/sample_metrics.xlsx`.
