package com.asm.eb;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ExceptionBuddyConfiguratorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldResolveConfigurationFilePath() throws Exception {
        File config = temporaryFolder.newFile("ebConfig.json");
        String expectedPath = config.getCanonicalPath();

        String configPath = ExceptionBuddyConfigurator.getConfigurationFile(
                "configurationFile=" + config.getAbsolutePath()
        );

        assertEquals(expectedPath, configPath);
    }

    @Test
    public void shouldRejectMissingConfigurationArgument() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ExceptionBuddyConfigurator.getConfigurationFile("pid=123")
        );

        assertEquals(
                "Missing required agent argument: configurationFile=<path-to-config-json>",
                exception.getMessage()
        );
    }

    @Test
    public void shouldRejectBlankAgentArgs() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ExceptionBuddyConfigurator.getConfigurationFile("   ")
        );

        assertEquals(
                "Missing agent arguments. Expected: configurationFile=<path-to-config-json>",
                exception.getMessage()
        );
    }
}
