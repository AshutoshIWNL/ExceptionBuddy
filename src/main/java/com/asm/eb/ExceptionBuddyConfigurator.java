package com.asm.eb;

import java.io.File;

/**
 * @author asmishra
 * @since 2/13/2025
 */
public class ExceptionBuddyConfigurator {
    public static String getConfigurationFile(String agentArgs) {
        String configurationFile = null;
        if(agentArgs != null) {
            String[] args = agentArgs.split(",");
            for(String arg : args) {
                if(arg.contains("configurationFile")) {
                    String[] prop = arg.split("=");
                    if(prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        configurationFile = prop[1];
                        File configFileObj = new File(configurationFile);
                        if(!configFileObj.exists()) {
                            throw new IllegalArgumentException("Config file doesn't exist in the specified directory - " + configurationFile);
                        }
                    }
                }
            }
        }
        return configurationFile;
    }
}
