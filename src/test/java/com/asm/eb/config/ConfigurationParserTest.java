package com.asm.eb.config;

import com.asm.eb.model.Configuration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ConfigurationParserTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldParseAndNormalizeConfiguration() throws Exception {
        File configFile = writeConfig("{\n" +
                "  \"useFilters\": true,\n" +
                "  \"filters\": [\" com.foo \", \"\", \"  \", \"com.bar\"],\n" +
                "  \"logFilePath\": \"  " + escapedPath("logs/eb.log") + "  \",\n" +
                "  \"classLoaderTracing\": false,\n" +
                "  \"exceptionMonitoring\": true,\n" +
                "  \"cnfSkipString\": \"   java.lang.ClassLoader.loadClass(ClassLoader.java:406)  \"\n" +
                "}\n");

        Configuration configuration = ConfigurationParser.parseConfigurationFile(configFile.getAbsolutePath());

        assertEquals(2, configuration.getFilters().size());
        assertTrue(configuration.getFilters().contains("com.foo"));
        assertTrue(configuration.getFilters().contains("com.bar"));
        assertEquals(temporaryFolder.getRoot().toPath().resolve("logs/eb.log").toString(), configuration.getLogFilePath());
        assertEquals("java.lang.ClassLoader.loadClass(ClassLoader.java:406)", configuration.getCnfSkipString());
    }

    @Test
    public void shouldRejectMissingLogFilePath() throws Exception {
        File configFile = writeConfig("{\n" +
                "  \"useFilters\": false,\n" +
                "  \"classLoaderTracing\": false,\n" +
                "  \"exceptionMonitoring\": false\n" +
                "}\n");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ConfigurationParser.parseConfigurationFile(configFile.getAbsolutePath())
        );

        assertEquals("Configuration property 'logFilePath' is required and cannot be blank.", exception.getMessage());
    }

    @Test
    public void shouldRejectEmptyFiltersWhenUseFiltersEnabled() throws Exception {
        File configFile = writeConfig("{\n" +
                "  \"useFilters\": true,\n" +
                "  \"filters\": [\"   \", \"\"],\n" +
                "  \"logFilePath\": \"" + escapedPath("logs/eb.log") + "\"\n" +
                "}\n");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ConfigurationParser.parseConfigurationFile(configFile.getAbsolutePath())
        );

        assertEquals("Configuration property 'filters' must contain at least one value when 'useFilters' is true.", exception.getMessage());
    }

    @Test
    public void shouldNullOutFiltersWhenUseFiltersDisabled() throws Exception {
        File configFile = writeConfig("{\n" +
                "  \"useFilters\": false,\n" +
                "  \"filters\": [\"com.foo\"],\n" +
                "  \"logFilePath\": \"" + escapedPath("logs/eb.log") + "\"\n" +
                "}\n");

        Configuration configuration = ConfigurationParser.parseConfigurationFile(configFile.getAbsolutePath());
        assertNull(configuration.getFilters());
    }

    private File writeConfig(String content) throws IOException {
        File configFile = temporaryFolder.newFile("ebConfig.json");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(content);
        }
        return configFile;
    }

    private String escapedPath(String relativePath) {
        return temporaryFolder.getRoot().toPath().resolve(relativePath).toString().replace("\\", "\\\\");
    }
}
