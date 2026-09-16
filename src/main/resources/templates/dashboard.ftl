<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Metrics Dashboard</title>
    <style>
        :root {
            --bg: #f4f7fb;
            --card: #ffffff;
            --text: #1f2937;
            --muted: #6b7280;
            --green: #15803d;
            --green-bg: #dcfce7;
            --yellow: #a16207;
            --yellow-bg: #fef3c7;
            --red: #b91c1c;
            --red-bg: #fee2e2;
            --blue: #1d4ed8;
            --border: #dbe4f0;
            --shadow: 0 10px 25px rgba(15, 23, 42, 0.08);
        }
        * { box-sizing: border-box; }
        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: var(--bg);
            color: var(--text);
        }
        .container {
            max-width: 1380px;
            margin: 0 auto;
            padding: 24px;
        }
        .header {
            display: flex;
            justify-content: space-between;
            gap: 16px;
            flex-wrap: wrap;
            margin-bottom: 24px;
        }
        .title-block h1 {
            margin: 0 0 8px;
            font-size: 2rem;
        }
        .title-block p, .meta { margin: 0; color: var(--muted); }
        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
            gap: 16px;
        }
        .grid.two-up { grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); }
        .grid.charts { grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); }
        .card {
            background: var(--card);
            border: 1px solid var(--border);
            border-radius: 16px;
            padding: 20px;
            box-shadow: var(--shadow);
        }
        .card h2, .card h3 {
            margin-top: 0;
        }
        .metric {
            display: flex;
            justify-content: space-between;
            gap: 12px;
            padding: 8px 0;
            border-bottom: 1px solid #eef2f7;
        }
        .metric:last-child { border-bottom: 0; }
        .metric-label { color: var(--muted); }
        .metric-value { font-weight: 700; }
        .summary-cards .card { padding: 18px; }
        .summary-value { font-size: 1.8rem; font-weight: 700; margin: 8px 0; }
        .pill, .status-pill {
            display: inline-block;
            border-radius: 999px;
            padding: 6px 10px;
            font-size: 0.8rem;
            font-weight: 700;
        }
        .status-green { color: var(--green); background: var(--green-bg); }
        .status-yellow { color: var(--yellow); background: var(--yellow-bg); }
        .status-red { color: var(--red); background: var(--red-bg); }
        .pill-blue { color: var(--blue); background: #dbeafe; }
        .kpi-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
            gap: 16px;
        }
        .kpi-card .summary-value { font-size: 1.5rem; }
        table {
            width: 100%;
            border-collapse: collapse;
        }
        th, td {
            padding: 12px;
            border-bottom: 1px solid #eef2f7;
            text-align: left;
        }
        th[data-sort] {
            cursor: pointer;
            user-select: none;
        }
        th[data-sort]:after {
            content: ' ⇅';
            color: var(--muted);
            font-size: 0.8rem;
        }
        canvas {
            width: 100%;
            height: 280px;
            border-radius: 12px;
            background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
        }
        .governance {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 16px;
        }
        .trend-up { color: var(--green); }
        .trend-down { color: var(--red); }
        @media print {
            body { background: #fff; }
            .card { box-shadow: none; }
        }
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <div class="title-block">
            <h1>AI Productivity Dashboard</h1>
            <p>Interactive portfolio view of QA baseline, AI uplift, KPIs, and governance health.</p>
        </div>
        <div class="meta">
            <div>Generated: ${generatedAt}</div>
            <div>Projects: ${projects?size}</div>
        </div>
    </div>

    <div class="grid summary-cards">
        <div class="card">
            <div class="metric-label">Total Stories</div>
            <div class="summary-value">${executiveSummary.totalStories?string["0.##"]}</div>
        </div>
        <div class="card">
            <div class="metric-label">Total Tests Generated</div>
            <div class="summary-value">${executiveSummary.totalTestsGenerated?string["0.##"]}</div>
        </div>
        <div class="card">
            <div class="metric-label">AI Generated %</div>
            <div class="summary-value">${executiveSummary.aiGeneratedPercentage?string["0.##"]}%</div>
        </div>
        <div class="card">
            <div class="metric-label">Overall Productivity Gain</div>
            <div class="summary-value">${executiveSummary.overallProductivityGain?string["0.##"]}%</div>
        </div>
        <div class="card">
            <div class="metric-label">Hours Saved</div>
            <div class="summary-value">${executiveSummary.hoursSaved?string["0.##"]}</div>
        </div>
        <div class="card">
            <div class="metric-label">Cost Avoidance</div>
            <div class="summary-value">$${executiveSummary.costAvoidance?string["0.##"]}</div>
        </div>
    </div>

    <div class="grid two-up" style="margin-top: 24px;">
        <div class="card">
            <h2>Baseline Metrics</h2>
            <div class="metric"><span class="metric-label">Test Cases Created / Month</span><span class="metric-value">${baselineSummary.testCasesCreated?string["0.##"]}</span></div>
            <div class="metric"><span class="metric-label">Test Design Productivity / QA Day</span><span class="metric-value">${baselineSummary.productivityPerDay?string["0.##"]}</span></div>
            <div class="metric"><span class="metric-label">Requirement Coverage</span><span class="metric-value">${baselineSummary.requirementCoverage?string["0.##"]}%</span></div>
            <div class="metric"><span class="metric-label">Defect Yield</span><span class="metric-value">${baselineSummary.defectYield?string["0.##"]}%</span></div>
            <div class="metric"><span class="metric-label">Rework Rate</span><span class="metric-value">${baselineSummary.reworkRate?string["0.##"]}%</span></div>
            <div class="metric"><span class="metric-label">Review Effort / 100 Cases</span><span class="metric-value">${baselineSummary.reviewEffort?string["0.##"]}h</span></div>
        </div>
        <div class="card">
            <h2>AI Productivity Metrics</h2>
            <div class="metric"><span class="metric-label">AI Generated Test Cases</span><span class="metric-value">${aiProductivitySummary.aiGeneratedTestCases?string["0.##"]}</span></div>
            <div class="metric"><span class="metric-label">Tests / Hour Before AI</span><span class="metric-value">${aiProductivitySummary.testsPerHourBefore?string["0.##"]}</span></div>
            <div class="metric"><span class="metric-label">Tests / Hour After AI</span><span class="metric-value">${aiProductivitySummary.testsPerHourAfter?string["0.##"]}</span></div>
            <div class="metric"><span class="metric-label">Productivity Gain</span><span class="metric-value">${aiProductivitySummary.productivityGain?string["0.##"]}%</span></div>
            <div class="metric"><span class="metric-label">AI Adoption Rate</span><span class="metric-value">${aiProductivitySummary.aiAdoptionRate?string["0.##"]}%</span></div>
        </div>
    </div>

    <div class="card" style="margin-top: 24px;">
        <h2>KPI Scorecard</h2>
        <div class="kpi-grid">
            <#list kpis as kpi>
                <div class="card kpi-card">
                    <span class="status-pill status-${kpi.statusClass}">${kpi.status}</span>
                    <h3>${kpi.name}</h3>
                    <div class="summary-value">${kpi.currentValue?string["0.##"]}${kpi.unit}</div>
                    <div class="metric-label">Target: ${kpi.target?string["0.##"]}${kpi.unit}</div>
                    <p class="metric-label">${kpi.description}</p>
                </div>
            </#list>
        </div>
    </div>

    <div class="card" style="margin-top: 24px;">
        <h2>Monthly Performance Table</h2>
        <table id="performance-table">
            <thead>
            <tr>
                <th data-sort="string">Application name</th>
                <th data-sort="number">Stories analyzed</th>
                <th data-sort="number">Tests Generated</th>
                <th data-sort="number">AI Generated count</th>
                <th data-sort="number">Productivity Gain %</th>
                <th data-sort="number">Coverage %</th>
                <th data-sort="number">Automation Candidate %</th>
            </tr>
            </thead>
            <tbody>
            <#list projects as project>
                <tr>
                    <td>${project.projectName}</td>
                    <td>${project.aiMetrics.storiesAnalyzed?string["0.##"]}</td>
                    <td>${project.totalTestsGenerated?string["0.##"]}</td>
                    <td>${project.aiGeneratedCount?string["0.##"]}</td>
                    <td>${project.productivityGain?string["0.##"]}</td>
                    <td>${project.requirementCoverage?string["0.##"]}</td>
                    <td>${project.automationCandidatePercentage?string["0.##"]}</td>
                </tr>
            </#list>
            </tbody>
        </table>
    </div>

    <div class="card" style="margin-top: 24px;">
        <h2>Governance Metrics</h2>
        <div class="governance">
            <div>
                <div class="metric-label">Maturity Level</div>
                <div class="summary-value">${governanceSummary.maturityLevel}</div>
                <span class="pill pill-blue">Governance</span>
            </div>
            <div>
                <div class="metric-label">Quality Score</div>
                <div class="summary-value">${governanceSummary.qualityScore?string["0.##"]}</div>
            </div>
            <div>
                <div class="metric-label">Productivity Trend</div>
                <div class="summary-value ${(governanceSummary.productivityTrend >= 0)?then('trend-up','trend-down')}">${(governanceSummary.productivityTrend >= 0)?then('▲ Improving','▼ Watchlist')}</div>
            </div>
            <div>
                <div class="metric-label">Coverage Trend</div>
                <div class="summary-value ${(governanceSummary.coverageTrend >= 0)?then('trend-up','trend-down')}">${(governanceSummary.coverageTrend >= 0)?then('▲ Healthy','▼ Needs action')}</div>
            </div>
            <div>
                <div class="metric-label">Review Trend</div>
                <div class="summary-value ${(governanceSummary.reviewTrend >= 0)?then('trend-up','trend-down')}">${(governanceSummary.reviewTrend >= 0)?then('▲ Efficient','▼ High effort')}</div>
            </div>
        </div>
    </div>

    <div class="grid charts" style="margin-top: 24px;">
        <div class="card"><h3>Productivity Gain Chart</h3><canvas id="productivity-chart" width="520" height="280"></canvas></div>
        <div class="card"><h3>AI Adoption Trend Chart</h3><canvas id="adoption-chart" width="520" height="280"></canvas></div>
        <div class="card"><h3>Coverage % by Application</h3><canvas id="coverage-chart" width="520" height="280"></canvas></div>
        <div class="card"><h3>Automation Readiness Distribution</h3><canvas id="automation-chart" width="520" height="280"></canvas></div>
        <div class="card"><h3>Cost Avoidance Breakdown</h3><canvas id="cost-chart" width="520" height="280"></canvas></div>
    </div>
</div>

<script>
const chartDataBase64 = '${chartsJsonBase64}';
const chartData = JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(chartDataBase64), ch => ch.charCodeAt(0))));

function renderBarChart(canvasId, labels, values, options) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    const padding = 40;
    const barWidth = Math.max(22, (width - padding * 2) / Math.max(labels.length * 1.6, 1));
    const maxValue = Math.max(...values, options.maxValue || 0, 1);

    ctx.clearRect(0, 0, width, height);
    ctx.fillStyle = '#334155';
    ctx.font = '12px Arial';
    ctx.textAlign = 'center';
    ctx.beginPath();
    ctx.moveTo(padding, padding / 2);
    ctx.lineTo(padding, height - padding);
    ctx.lineTo(width - padding / 2, height - padding);
    ctx.strokeStyle = '#cbd5e1';
    ctx.stroke();

    values.forEach((value, index) => {
        const x = padding + index * (barWidth * 1.4) + 16;
        const scaledHeight = ((height - padding * 1.8) * value) / maxValue;
        const y = height - padding - scaledHeight;
        ctx.fillStyle = options.color;
        ctx.fillRect(x, y, barWidth, scaledHeight);
        ctx.fillStyle = '#0f172a';
        ctx.fillText(value.toFixed(1), x + barWidth / 2, y - 8);
        ctx.save();
        ctx.translate(x + barWidth / 2, height - padding + 12);
        ctx.rotate(-0.35);
        ctx.fillStyle = '#475569';
        ctx.fillText(labels[index], 0, 0);
        ctx.restore();
    });
}

function renderDualBarChart(canvasId, labels, firstValues, secondValues) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const width = canvas.width;
    const height = canvas.height;
    const padding = 40;
    const groupWidth = Math.max(40, (width - padding * 2) / Math.max(labels.length, 1));
    const barWidth = groupWidth / 3;
    const maxValue = Math.max(...firstValues, ...secondValues, 1);

    ctx.clearRect(0, 0, width, height);
    ctx.font = '12px Arial';
    ctx.beginPath();
    ctx.moveTo(padding, padding / 2);
    ctx.lineTo(padding, height - padding);
    ctx.lineTo(width - padding / 2, height - padding);
    ctx.strokeStyle = '#cbd5e1';
    ctx.stroke();

    labels.forEach((label, index) => {
        const baseX = padding + index * groupWidth + 16;
        const firstHeight = ((height - padding * 1.8) * firstValues[index]) / maxValue;
        const secondHeight = ((height - padding * 1.8) * secondValues[index]) / maxValue;
        ctx.fillStyle = '#93c5fd';
        ctx.fillRect(baseX, height - padding - firstHeight, barWidth, firstHeight);
        ctx.fillStyle = '#2563eb';
        ctx.fillRect(baseX + barWidth + 6, height - padding - secondHeight, barWidth, secondHeight);
        ctx.fillStyle = '#0f172a';
        ctx.textAlign = 'center';
        ctx.fillText(label, baseX + barWidth, height - 10);
    });

    ctx.fillStyle = '#93c5fd';
    ctx.fillRect(width - 155, 12, 12, 12);
    ctx.fillStyle = '#0f172a';
    ctx.fillText('Before AI', width - 105, 22);
    ctx.fillStyle = '#2563eb';
    ctx.fillRect(width - 75, 12, 12, 12);
    ctx.fillStyle = '#0f172a';
    ctx.fillText('After AI', width - 25, 22);
}

renderDualBarChart('productivity-chart', chartData.labels, chartData.baselineProductivity, chartData.aiProductivity);
renderBarChart('adoption-chart', chartData.labels, chartData.adoptionRates, { color: '#7c3aed', maxValue: 100 });
renderBarChart('coverage-chart', chartData.labels, chartData.coverageValues, { color: '#0891b2', maxValue: 100 });
renderBarChart('automation-chart', chartData.labels, chartData.automationValues, { color: '#ea580c', maxValue: 100 });
renderBarChart('cost-chart', chartData.labels, chartData.costAvoidanceValues, { color: '#16a34a' });

(function enableSorting() {
    const table = document.getElementById('performance-table');
    if (!table) return;
    const headers = table.querySelectorAll('th[data-sort]');
    headers.forEach((header, index) => {
        header.addEventListener('click', () => {
            const rows = Array.from(table.querySelectorAll('tbody tr'));
            const isNumber = header.dataset.sort === 'number';
            const direction = header.dataset.direction === 'asc' ? 'desc' : 'asc';
            header.dataset.direction = direction;
            rows.sort((left, right) => {
                const leftValue = left.children[index].textContent.trim();
                const rightValue = right.children[index].textContent.trim();
                const a = isNumber ? parseFloat(leftValue) : leftValue.toLowerCase();
                const b = isNumber ? parseFloat(rightValue) : rightValue.toLowerCase();
                if (a < b) return direction === 'asc' ? -1 : 1;
                if (a > b) return direction === 'asc' ? 1 : -1;
                return 0;
            });
            const tbody = table.querySelector('tbody');
            rows.forEach(row => tbody.appendChild(row));
        });
    });
})();
</script>
</body>
</html>
