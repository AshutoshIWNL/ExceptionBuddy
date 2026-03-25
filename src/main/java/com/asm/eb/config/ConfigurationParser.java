package com.asm.eb.config;

import com.asm.eb.model.Configuration;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the configuration file and returns a Configuration object.
 * Uses Jackson for JSON deserialization.
 *
 * @author asmishra
 * @since 2/13/2025
 */
public class ConfigurationParser {
    public static Configuration parseConfigurationFile(String configurationFile) {
        if (configurationFile == null || configurationFile.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration file path cannot be null or blank.");
        }

        Configuration configuration;
        ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
        try {
            File configurationFileObject = new File(configurationFile);
            configuration = objectMapper.readValue(configurationFileObject, Configuration.class);
        } catch (IOException ioe) {
            System.err.println("Failed to parse configuration file: " + ioe.getMessage());
            throw new RuntimeException("Failed to parse configuration file: " + ioe.getMessage(), ioe);
        }
        return validateAndNormalize(configuration);
    }

    private static Configuration validateAndNormalize(Configuration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration payload is empty.");
        }

        String logFilePath = configuration.getLogFilePath();
        if (logFilePath == null || logFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration property 'logFilePath' is required and cannot be blank.");
        }
        configuration.setLogFilePath(logFilePath.trim());

        List<String> filters = configuration.getFilters();
        if (filters != null) {
            List<String> normalizedFilters = new ArrayList<>();
            for (String filter : filters) {
                if (filter != null) {
                    String normalized = filter.trim();
                    if (!normalized.isEmpty()) {
                        normalizedFilters.add(normalized);
                    }
                }
            }
            configuration.setFilters(normalizedFilters);
        }

        if (configuration.isUseFilters() && (configuration.getFilters() == null || configuration.getFilters().isEmpty())) {
            throw new IllegalArgumentException("Configuration property 'filters' must contain at least one value when 'useFilters' is true.");
        }
        if (!configuration.isUseFilters()) {
            configuration.setFilters(null);
        }

        String cnfSkipString = configuration.getCnfSkipString();
        if (cnfSkipString != null) {
            String normalized = cnfSkipString.trim();
            configuration.setCnfSkipString(normalized.isEmpty() ? null : normalized);
        }

        return configuration;
    }
}
