package com.asm.eb.attach;

import com.sun.tools.attach.VirtualMachine;
import org.apache.commons.cli.*;

import java.io.File;

/**
 * A command-line interface (CLI) tool for attaching a Java agent to a running JVM.
 * This class provides a way to attach an agent JAR file to a target Java process
 * identified by its PID. It uses command-line options to specify the agent JAR path,
 * configuration file, additional arguments, and the target JVM PID.
 *
 * @author asmishra
 * @since 2/18/2025
 */
public class AgentAttachCLI {
    private static final String AGENT_JAR_OPTION = "agentJar";
    private static final String CONFIGURATION_FILE_OPTION = "configurationFile";
    private static final String PID_OPTION = "pid";

    public static void main(String[] args) {
        Options options = buildOptions();
        try {
            CliArguments cliArguments = parseAndValidateArguments(args, options);
            attachAgent(cliArguments.agentJarPath, cliArguments.configFilePath, cliArguments.targetPid);
        } catch (ParseException e) {
            System.err.println("Error parsing command-line arguments: " + e.getMessage());
            printUsage(options);
        } catch (Exception e) {
            System.err.println("Error while attaching agent to the target JVM: " + e.getMessage());
        }
    }

    static Options buildOptions() {
        Options options = new Options();

        Option agentJarOption = Option.builder()
                .longOpt(AGENT_JAR_OPTION)
                .hasArg(true)
                .required(true)
                .desc("Path to the agent JAR file")
                .build();
        Option configFileOption = Option.builder()
                .longOpt(CONFIGURATION_FILE_OPTION)
                .hasArg(true)
                .required(true)
                .desc("Path to the agent config file")
                .build();
        Option pidOption = Option.builder()
                .longOpt(PID_OPTION)
                .hasArg(true)
                .required(true)
                .desc("PID of the target JVM")
                .build();
        options.addOption(agentJarOption);
        options.addOption(configFileOption);
        options.addOption(pidOption);
        return options;
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -cp <agent-jar> com.asm.eb.attach.AgentAttachCLI --agentJar <path> --configurationFile <path> --pid <pid>", options);
    }

    static CliArguments parseAndValidateArguments(String[] args, Options options) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String agentJarPath = validateReadableFile(cmd.getOptionValue(AGENT_JAR_OPTION), "agentJar");
        String configFilePath = validateReadableFile(cmd.getOptionValue(CONFIGURATION_FILE_OPTION), "configurationFile");
        String targetPid = validatePid(cmd.getOptionValue(PID_OPTION));

        return new CliArguments(agentJarPath, configFilePath, targetPid);
    }

    private static String validateReadableFile(String path, String optionName) throws ParseException {
        if (path == null || path.trim().isEmpty()) {
            throw new ParseException("Option '" + optionName + "' must be provided.");
        }

        File file = new File(path.trim());
        if (!file.exists()) {
            throw new ParseException("Option '" + optionName + "' points to a non-existent path: " + file.getPath());
        }
        if (!file.isFile()) {
            throw new ParseException("Option '" + optionName + "' must point to a file: " + file.getPath());
        }
        if (!file.canRead()) {
            throw new ParseException("Option '" + optionName + "' file is not readable: " + file.getPath());
        }
        return file.getAbsolutePath();
    }

    private static String validatePid(String pid) throws ParseException {
        if (pid == null || pid.trim().isEmpty()) {
            throw new ParseException("Option 'pid' must be provided.");
        }
        String normalizedPid = pid.trim();
        try {
            long parsedPid = Long.parseLong(normalizedPid);
            if (parsedPid <= 0) {
                throw new ParseException("Option 'pid' must be a positive integer.");
            }
        } catch (NumberFormatException e) {
            throw new ParseException("Option 'pid' must be numeric.");
        }
        return normalizedPid;
    }

    /**
     * Attaches the specified agent to a target JVM identified by its PID.
     *
     * @param agentFilePath The file path to the agent JAR.
     * @param configFile    The path to the configuration file for the agent.
     * @param targetPID     The PID of the target JVM to which the agent will be attached.
     * @throws Exception If there is an error attaching the agent to the target JVM.
     */
    private static void attachAgent(String agentFilePath, String configFile, String targetPID) throws Exception {
        VirtualMachine vm = null;
        String args = "configurationFile=" + configFile;
        try {
            vm = VirtualMachine.attach(targetPID);
            vm.loadAgent(new File(agentFilePath).getAbsolutePath(), args);
            System.out.println("Agent attached successfully to PID " + targetPID);
        } finally {
            if (vm != null) {
                vm.detach();
            }
        }
    }

    static final class CliArguments {
        final String agentJarPath;
        final String configFilePath;
        final String targetPid;

        private CliArguments(String agentJarPath, String configFilePath, String targetPid) {
            this.agentJarPath = agentJarPath;
            this.configFilePath = configFilePath;
            this.targetPid = targetPid;
        }
    }
}
