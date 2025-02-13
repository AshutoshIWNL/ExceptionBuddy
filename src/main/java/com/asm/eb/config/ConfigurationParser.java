package com.asm.eb.config;

import com.asm.eb.model.Configuration;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
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
            System.err.println("Configuration file parsing failed due to : " + ioe.getMessage());
            throw new RuntimeException("Configuration file parsing failed");
        }
        return configuration;
    }
}
