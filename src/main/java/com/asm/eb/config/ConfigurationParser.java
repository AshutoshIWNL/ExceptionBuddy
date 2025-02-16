package com.asm.eb.config;

import com.asm.eb.model.Configuration;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * Parses the configuration file and returns a Configuration object.
 * Uses Jackson for JSON deserialization.
 *
 * @author asmishra
 * @since 2/13/2025
 */
public class ConfigurationParser {
    public static Configuration parseConfigurationFile(String configurationFile) {
        Configuration configuration = null;
        ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
        try {
            File configurationFileObject = new File(configurationFile);
            configuration = objectMapper.readValue(configurationFileObject, Configuration.class);
        } catch (IOException ioe) {
            System.err.println("Failed to parse configuration file: " + ioe.getMessage());
            throw new RuntimeException("Failed to parse configuration file: " + ioe.getMessage(), ioe);
        }
        return configuration;
    }
}
