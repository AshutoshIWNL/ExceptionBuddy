package com.asm.eb;

import com.asm.eb.logger.ExceptionLogger;
import com.asm.eb.monitor.JVMExceptionMonitor;
import com.asm.eb.transformer.ExceptionTransformer;
import com.asm.eb.config.ConfigurationParser;
import com.asm.eb.model.Configuration;
import com.asm.eb.util.JVMUtils;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URISyntaxException;
import java.util.jar.JarFile;

/**
 * Entry point for the ExceptionBuddy Java agent.
 * This agent instruments exceptions at runtime and provides monitoring capabilities.
 *
 * @author asmishra
 * @since 2/13/2025
 */
public class ExceptionBuddy {

    private static final String EXCEPTION_BUDDY_TAG = "[ExceptionBuddy]";
    private static final String THROWABLE_CLASS_NAME = "java.lang.Throwable";
    private static final String PREMAIN_MODE = "javaagent";
    private static final String AGENTMAIN_MODE = "attachVM";

    /**
     * The premain method is executed before the main application starts.
     *
     * @param agentArgs Arguments passed to the agent.
     * @param inst      The instrumentation instance.
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            File agentJar = new File(ExceptionBuddy.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            //Since we are dealing with java.lang.Throwable transformation, Boostrap classloader will be the one trying to load our agent classes downstream.
            //This will result in classloading issues due to visibility principle.
            //Normal approach is to use -Xbootclasspath but to avoid unnecessary JVM options setup, we are using the below to append our agent jar to bootstrap's classpath.
            inst.appendToBootstrapClassLoaderSearch(new JarFile(agentJar));
        } catch (Exception e) {
            System.err.println(EXCEPTION_BUDDY_TAG + " failure in premain: " + e.getMessage());
            return;
        }
        instrument(agentArgs, inst, PREMAIN_MODE);
    }

    /**
     * The agentmain method is executed for runtime attachments.
     *
     * @param agentArgs Arguments passed to the agent.
     * @param inst      The instrumentation instance.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        try {
            File agentJar = new File(ExceptionBuddy.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            //Since we are dealing with java.lang.Throwable transformation, Boostrap classloader will be the one trying to load our agent classes downstream.
            //This will result in classloading issues due to visibility principle.
            //Normal approach is to use -Xbootclasspath but since it is runtime, we are using the below to append our agent jar to bootstrap's classpath.
            inst.appendToBootstrapClassLoaderSearch(new JarFile(agentJar));
        } catch (Exception e) {
            System.err.println(EXCEPTION_BUDDY_TAG + " failure in agentmain: " + e.getMessage());
            return;
        }
        instrument(agentArgs, inst, AGENTMAIN_MODE);
    }

    /**
     * Handles the instrumentation process by configuring exception logging and monitoring.
     *
     * @param agentArgs Arguments passed to the agent.
     * @param inst      The instrumentation instance.
     */
    private static void instrument(String agentArgs, Instrumentation inst, String mode) {
        System.out.println(EXCEPTION_BUDDY_TAG + " <" + mode + "> Initialization started...");
        String configurationFile = null;
        try {
            configurationFile = ExceptionBuddyConfigurator.getConfigurationFile(agentArgs);
        } catch (Exception e) {
            System.err.println(EXCEPTION_BUDDY_TAG + " Failed to retrieve configuration file: " + e.getMessage());
            return;
        }

        Configuration configuration = null;
        try {
            configuration = ConfigurationParser.parseConfigurationFile(configurationFile);
        } catch (Exception e) {
            System.err.println(EXCEPTION_BUDDY_TAG + " Error parsing configuration file: " + e.getMessage());
            return;
        }

        ExceptionLogger exceptionLogger = configuration.isUseFilters()
                ? ExceptionLogger.getInstance(configuration.getLogFilePath(), configuration.getFilters(), configuration.isExceptionMonitoring(), configuration.getCnfSkipString())
                : ExceptionLogger.getInstance(configuration.getLogFilePath(), null, configuration.isExceptionMonitoring(), configuration.getCnfSkipString());

        exceptionLogger.logInfo("Exception Buddy initialized successfully.");
        exceptionLogger.logInfo(JVMUtils.getJVMCommandLine());
        if(configuration.isPrintJVMSysProps())
            exceptionLogger.logInfo(JVMUtils.getJVMSystemProperties());
        if(configuration.isPrintEnvironmentVariables())
            exceptionLogger.logInfo(JVMUtils.getEnvVars());

        ExceptionTransformer exceptionTransformer = new ExceptionTransformer(configuration, exceptionLogger);

        // Check if class retransformation is supported and apply transformation to Throwable
        if(inst.isRetransformClassesSupported()) {
            inst.addTransformer(exceptionTransformer, true);
            try {
                Class<?> throwableClass = Class.forName(THROWABLE_CLASS_NAME);
                inst.retransformClasses(throwableClass);
            } catch (ClassNotFoundException e) {
                exceptionLogger.logError("Throwable class not found: " + e.getMessage());
            } catch (UnmodifiableClassException e) {
                exceptionLogger.logError("Cannot redefine Throwable class: " + e.getMessage());
            }
        } else {
            exceptionLogger.logError("Class re-transformation is not supported. Exiting.");
            return;
        }
        if(configuration.isExceptionMonitoring()) {
            JVMExceptionMonitor jvmExceptionMonitor = JVMExceptionMonitor.getInstance(exceptionLogger);
            jvmExceptionMonitor.execute();
        }

        Thread shutdownHook = new Thread(() -> {
            JVMExceptionMonitor jvmExceptionMonitor = JVMExceptionMonitor.getInstance();
            if (jvmExceptionMonitor != null)
                jvmExceptionMonitor.shutdown();
            ExceptionLogger.getInstance().close();
            System.out.println(EXCEPTION_BUDDY_TAG + " Shutdown complete.");
        });
        shutdownHook.setName("eb-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

}
