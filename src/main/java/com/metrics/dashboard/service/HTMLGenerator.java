package com.metrics.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrics.dashboard.exception.GenerationException;
import com.metrics.dashboard.model.DashboardData;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Renders dashboard HTML using the Freemarker template. */
public class HTMLGenerator {
    private final Configuration configuration;
    private final ObjectMapper objectMapper;

    public HTMLGenerator() {
        this.configuration = new Configuration(Configuration.VERSION_2_3_34);
        this.configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/templates");
        this.configuration.setDefaultEncoding("UTF-8");
        this.objectMapper = new ObjectMapper();
    }

    /** Generates the HTML dashboard at the supplied output path. */
    public void generate(DashboardData dashboardData, Path outputPath) {
        try {
            Template template = configuration.getTemplate("dashboard.ftl");
            Map<String, Object> templateModel = objectMapper.convertValue(dashboardData, new TypeReference<>() { });
            templateModel.put("chartsJson", objectMapper.writeValueAsString(dashboardData.charts()));
            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(outputPath)) {
                template.process(templateModel, writer);
            }
        } catch (IOException | TemplateException exception) {
            throw new GenerationException("Failed to generate HTML dashboard at: " + outputPath, exception);
        }
    }
}
