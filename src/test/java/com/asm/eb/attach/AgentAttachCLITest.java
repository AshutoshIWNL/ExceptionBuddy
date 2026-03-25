package com.asm.eb.attach;

import org.apache.commons.cli.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class AgentAttachCLITest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldParseValidArguments() throws Exception {
        File agentJar = temporaryFolder.newFile("ExceptionBuddy.jar");
        File config = temporaryFolder.newFile("ebConfig.json");

        AgentAttachCLI.CliArguments cliArguments = AgentAttachCLI.parseAndValidateArguments(
                new String[]{
                        "--agentJar", agentJar.getAbsolutePath(),
                        "--configurationFile", config.getAbsolutePath(),
                        "--pid", "1234"
                },
                AgentAttachCLI.buildOptions()
        );

        assertEquals(agentJar.getAbsolutePath(), cliArguments.agentJarPath);
        assertEquals(config.getAbsolutePath(), cliArguments.configFilePath);
        assertEquals("1234", cliArguments.targetPid);
    }

    @Test
    public void shouldRejectNonNumericPid() throws Exception {
        File agentJar = temporaryFolder.newFile("ExceptionBuddy.jar");
        File config = temporaryFolder.newFile("ebConfig.json");
        ParseException parseException = assertThrows(
                ParseException.class,
                () -> AgentAttachCLI.parseAndValidateArguments(
                        new String[]{
                                "--agentJar", agentJar.getAbsolutePath(),
                                "--configurationFile", config.getAbsolutePath(),
                                "--pid", "abc"
                        },
                        AgentAttachCLI.buildOptions()
                )
        );

        assertEquals("Option 'pid' must be numeric.", parseException.getMessage());
    }

    @Test
    public void shouldRejectMissingRequiredOption() {
        ParseException parseException = assertThrows(
                ParseException.class,
                () -> AgentAttachCLI.parseAndValidateArguments(
                        new String[]{
                                "--agentJar", "a.jar",
                                "--configurationFile", "b.json"
                        },
                        AgentAttachCLI.buildOptions()
                )
        );

        assertEquals("Missing required option: pid", parseException.getMessage());
    }
}
