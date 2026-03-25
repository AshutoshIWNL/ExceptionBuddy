package com.asm.eb;

import java.io.File;
import java.io.IOException;

/**
 * @author asmishra
 * @since 2/13/2025
 */
public class ExceptionBuddyConfigurator {
    private static final String CONFIGURATION_FILE_KEY = "configurationFile";

    public static String getConfigurationFile(String agentArgs) {
        if (agentArgs == null || agentArgs.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing agent arguments. Expected: configurationFile=<path-to-config-json>");
        }

        String configurationFile = null;
        String[] args = agentArgs.split(",");
        for (String arg : args) {
            String token = arg == null ? "" : arg.trim();
            if (token.isEmpty()) {
                continue;
            }

            String[] prop = token.split("=", 2);
            if (prop.length != 2) {
                throw new IllegalArgumentException("Invalid agent argument: " + token + ". Expected key=value.");
            }
            String key = prop[0].trim();
            String value = prop[1].trim();

            if (CONFIGURATION_FILE_KEY.equals(key)) {
                if (value.isEmpty()) {
                    throw new IllegalArgumentException("Agent argument 'configurationFile' cannot be blank.");
                }
                configurationFile = value;
            }
        }

        if (configurationFile == null) {
            throw new IllegalArgumentException("Missing required agent argument: configurationFile=<path-to-config-json>");
        }

        File configFileObj = new File(configurationFile);
        if (!configFileObj.exists()) {
            throw new IllegalArgumentException("Config file does not exist: " + configurationFile);
        }
        if (!configFileObj.isFile()) {
            throw new IllegalArgumentException("Config path is not a file: " + configurationFile);
        }
        if (!configFileObj.canRead()) {
            throw new IllegalArgumentException("Config file is not readable: " + configurationFile);
        }

        try {
            return configFileObj.getCanonicalPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to resolve canonical path for config file: " + configurationFile, e);
        }
    }
}
