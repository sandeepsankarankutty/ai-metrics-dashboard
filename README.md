# ai-metrics-dashboard

Java CLI for generating an interactive HTML dashboard from Excel-based QA and AI testing metrics.

## Build

```bash
mvn clean package
```

## Run

```bash
java -jar target/ai-metrics-dashboard-1.0.0-SNAPSHOT.jar --input sample_data/QA_Effort_Spent.xlsx --output dashboard.html
```

## Options

- `-i`, `--input` - input Excel workbook (`.xlsx`)
- `-o`, `--output` - output HTML file
- `-v`, `--verbose` - enable debug logging
- `-h`, `--help` - show help

## Workbook format

Use one baseline sheet with a name containing `Before AI` and one or more sheets containing `After AI`.

The parser supports the actual QA workbook layout: multi-column baseline metrics and per-project/week AI sheets (stories by complexity, AI interaction time, generated tests, coverage, review/rework, and quality indicators).

A sample workbook is included at `sample_data/QA_Effort_Spent.xlsx`.
