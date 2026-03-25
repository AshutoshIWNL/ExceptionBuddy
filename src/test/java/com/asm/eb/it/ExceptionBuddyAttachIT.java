package com.asm.eb.it;

import com.asm.eb.it.targets.ExceptionBuddyTargetApp;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExceptionBuddyAttachIT {

    private static final long PROCESS_TIMEOUT_SECONDS = 30L;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void startupAttachShouldLogExceptions() throws Exception {
        File logFile = temporaryFolder.newFile("startup.log");
        File configFile = writeConfig(logFile, false, null, false, false, false);

        RunningProcess process = startStartupAttachedProcess(configFile, "startup");
        assertTrue("Startup target process did not finish in time.", process.process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue("Startup target process failed. Output:\n" + process.joinedOutput(), process.process.exitValue() == 0);

        String logContents = readLog(logFile);
        assertTrue(logContents.contains("EB_STARTUP_EXCEPTION"));
    }

    @Test
    public void startupAttachShouldRespectFilters() throws Exception {
        File logFile = temporaryFolder.newFile("filter.log");
        File configFile = writeConfig(logFile, true, "com.asm.eb.it.targets.FilterHitGenerator", false, false, false);

        RunningProcess process = startStartupAttachedProcess(configFile, "filter");
        assertTrue("Filter target process did not finish in time.", process.process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue("Filter target process failed. Output:\n" + process.joinedOutput(), process.process.exitValue() == 0);

        String logContents = readLog(logFile);
        assertTrue(logContents.contains("EB_FILTER_HIT"));
        assertFalse(logContents.contains("EB_FILTER_MISS"));
    }

    @Test
    public void startupAttachShouldEmitOptionalRuntimeDataWhenEnabled() throws Exception {
        File logFile = temporaryFolder.newFile("flags.log");
        File configFile = writeConfig(logFile, false, null, false, true, true);

        RunningProcess process = startStartupAttachedProcess(configFile, "startup");
        assertTrue("Flags target process did not finish in time.", process.process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue("Flags target process failed. Output:\n" + process.joinedOutput(), process.process.exitValue() == 0);

        String logContents = readLog(logFile);
        assertTrue(logContents.contains("*******JVM System Properties*******"));
        assertTrue(logContents.contains("*******Environment Variables*******"));
    }

    @Test
    public void runtimeAttachShouldLogExceptionsAfterAttach() throws Exception {
        File logFile = temporaryFolder.newFile("runtime.log");
        File configFile = writeConfig(logFile, false, null, false, false, false);

        RunningProcess runtimeProcess = startPlainProcess("runtime");
        String targetPid = waitForPid(runtimeProcess);

        RunningProcess attachProcess = startAttachProcess(configFile, targetPid);
        assertTrue("Attach process did not finish in time.", attachProcess.process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue("Attach process failed. Output:\n" + attachProcess.joinedOutput(), attachProcess.process.exitValue() == 0);

        assertTrue("Runtime target process did not finish in time.", runtimeProcess.process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue("Runtime target process failed. Output:\n" + runtimeProcess.joinedOutput(), runtimeProcess.process.exitValue() == 0);

        String logContents = readLog(logFile);
        assertTrue(logContents.contains("EB_RUNTIME_ATTACH"));
    }

    private RunningProcess startStartupAttachedProcess(File configFile, String mode) throws IOException {
        List<String> command = baseJavaCommand();
        command.add("-javaagent:" + agentJar().getAbsolutePath() + "=configurationFile=" + configFile.getAbsolutePath());
        command.add("-cp");
        command.add(testClasspath());
        command.add(ExceptionBuddyTargetApp.class.getName());
        command.add(mode);
        return startProcess(command);
    }

    private RunningProcess startPlainProcess(String mode) throws IOException {
        List<String> command = baseJavaCommand();
        command.add("-cp");
        command.add(testClasspath());
        command.add(ExceptionBuddyTargetApp.class.getName());
        command.add(mode);
        return startProcess(command);
    }

    private RunningProcess startAttachProcess(File configFile, String pid) throws IOException {
        List<String> command = baseJavaCommand();
        if (javaMajorVersion() >= 9) {
            command.add("--add-modules");
            command.add("jdk.attach");
        }
        command.add("-cp");
        command.add(attachClasspath());
        command.add("com.asm.eb.attach.AgentAttachCLI");
        command.add("--agentJar");
        command.add(agentJar().getAbsolutePath());
        command.add("--configurationFile");
        command.add(configFile.getAbsolutePath());
        command.add("--pid");
        command.add(pid);
        return startProcess(command);
    }

    private String waitForPid(RunningProcess process) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(PROCESS_TIMEOUT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            List<String> snapshot = process.snapshotOutput();
            for (String line : snapshot) {
                if (line.startsWith("PID=")) {
                    return line.substring("PID=".length()).trim();
                }
            }
            Thread.sleep(100L);
        }
        throw new IllegalStateException("Failed to discover target PID from process output.");
    }

    private RunningProcess startProcess(List<String> command) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        return new RunningProcess(process);
    }

    private File writeConfig(File logFile,
                             boolean useFilters,
                             String filterPrefix,
                             boolean exceptionMonitoring,
                             boolean printJvmSysProps,
                             boolean printEnvironmentVariables) throws IOException {
        File config = temporaryFolder.newFile("ebConfig-" + System.nanoTime() + ".json");
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"useFilters\": ").append(useFilters).append(",\n");
        if (useFilters) {
            json.append("  \"filters\": [\"").append(filterPrefix).append("\"],\n");
        } else {
            json.append("  \"filters\": [],\n");
        }
        json.append("  \"logFilePath\": \"").append(escape(logFile.getAbsolutePath())).append("\",\n");
        json.append("  \"classLoaderTracing\": false,\n");
        json.append("  \"exceptionMonitoring\": ").append(exceptionMonitoring).append(",\n");
        json.append("  \"cnfSkipString\": \"java.lang.ClassLoader.loadClass(ClassLoader.java:406)\",\n");
        json.append("  \"printJVMSysProps\": ").append(printJvmSysProps).append(",\n");
        json.append("  \"printEnvironmentVariables\": ").append(printEnvironmentVariables).append("\n");
        json.append("}\n");

        try (FileWriter writer = new FileWriter(config)) {
            writer.write(json.toString());
        }
        return config;
    }

    private String readLog(File logFile) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(PROCESS_TIMEOUT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            if (logFile.exists() && logFile.length() > 0) {
                return new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8);
            }
            Thread.sleep(200L);
        }
        throw new IllegalStateException("Log file was not produced in time: " + logFile.getAbsolutePath());
    }

    private List<String> baseJavaCommand() {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        return command;
    }

    private String javaExecutable() {
        String javaHome = System.getProperty("java.home");
        String executable = isWindows() ? "java.exe" : "java";
        return new File(new File(javaHome, "bin"), executable).getAbsolutePath();
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private int javaMajorVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, 3));
        }
        int dot = version.indexOf('.');
        String major = dot > 0 ? version.substring(0, dot) : version;
        return Integer.parseInt(major);
    }

    private File agentJar() {
        File targetDir = new File("target");
        File[] candidates = targetDir.listFiles(file ->
                file.isFile()
                        && file.getName().startsWith("ExceptionBuddy-")
                        && file.getName().endsWith(".jar")
                        && !file.getName().startsWith("original-"));
        if (candidates == null || candidates.length == 0) {
            throw new IllegalStateException("Agent jar not found for integration tests in: " + targetDir.getAbsolutePath());
        }
        Optional<File> latest = Optional.of(candidates[0]);
        for (File candidate : candidates) {
            if (candidate.lastModified() > latest.get().lastModified()) {
                latest = Optional.of(candidate);
            }
        }
        return latest.get();
    }

    private String testClasspath() {
        return new File("target", "test-classes").getAbsolutePath();
    }

    private String attachClasspath() {
        StringBuilder classpath = new StringBuilder(agentJar().getAbsolutePath());
        File toolsJar = findToolsJar();
        if (toolsJar != null) {
            classpath.append(File.pathSeparator).append(toolsJar.getAbsolutePath());
        }
        return classpath.toString();
    }

    private File findToolsJar() {
        File javaHome = new File(System.getProperty("java.home"));
        File toolsFromJavaHome = new File(new File(javaHome, "lib"), "tools.jar");
        if (toolsFromJavaHome.exists()) {
            return toolsFromJavaHome;
        }
        File parent = javaHome.getParentFile();
        if (parent != null) {
            File toolsFromParent = new File(new File(parent, "lib"), "tools.jar");
            if (toolsFromParent.exists()) {
                return toolsFromParent;
            }
        }
        return null;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\");
    }

    private static final class RunningProcess {
        private final Process process;
        private final List<String> outputLines = Collections.synchronizedList(new ArrayList<String>());

        private RunningProcess(Process process) {
            this.process = process;
            Thread gobbler = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputLines.add(line);
                    }
                } catch (IOException ignored) {
                    // no-op
                }
            }, "process-output-gobbler");
            gobbler.setDaemon(true);
            gobbler.start();
        }

        private List<String> snapshotOutput() {
            synchronized (outputLines) {
                return new ArrayList<>(outputLines);
            }
        }

        private String joinedOutput() {
            return String.join(System.lineSeparator(), snapshotOutput());
        }
    }
}
