package com.asm.eb;

import com.asm.eb.logger.ExceptionLogger;
import com.asm.eb.transformer.ExceptionTransformer;
import com.asm.eb.config.ConfigurationParser;
import com.asm.eb.model.Configuration;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * @author asmishra
 * @since 2/13/2025
 */
public class ExceptionBuddy {

    private static final String EXCEPTION_BUDDY_TAG = "[ExceptionBuddy]";
    public static void premain(String agentArgs, Instrumentation inst) {
        instrument(agentArgs, inst);
    }

    private static void instrument(String agentArgs, Instrumentation inst) {
        System.out.println(EXCEPTION_BUDDY_TAG + " Yo, we're live!");
        String configurationFile = null;
        try {
            configurationFile = ExceptionBuddyConfigurator.getConfigurationFile(agentArgs);
        } catch (Exception e) {
            System.err.println(EXCEPTION_BUDDY_TAG + " Big oops! Can't get the config file: " + e.getMessage());
            return;
        }

        Configuration configuration = null;
        try {
            configuration = ConfigurationParser.parseConfigurationFile(configurationFile);
        } catch (Exception e) {
            System.err.println(EXCEPTION_BUDDY_TAG + " Yikes, failed to parse config: " + e.getMessage());
            return;
        }

        ExceptionLogger exceptionLogger = null;
        if(configuration.isUseFilters()) {
            exceptionLogger = ExceptionLogger.getInstance(configuration.getLogFilePath(), configuration.getFilters());
        } else {
            exceptionLogger = ExceptionLogger.getInstance(configuration.getLogFilePath(), null);
        }

        exceptionLogger.logInfo("Exception Buddy is on and ready to roll!");

        ExceptionTransformer exceptionTransformer = new ExceptionTransformer(configuration, exceptionLogger);

        if(inst.isRetransformClassesSupported()) {
            inst.addTransformer(exceptionTransformer, true);
            try {
                Class<?> throwableClass = Class.forName("java.lang.Throwable");
                inst.retransformClasses(throwableClass);
            } catch (ClassNotFoundException e) {
                exceptionLogger.logError("Oops, class not found " + e.getMessage());
            } catch (UnmodifiableClassException e) {
                exceptionLogger.logError("Oops, can't redefine this class: " + e.getMessage());
            }
        } else {
            exceptionLogger.logError("Bummer! Class re-transformation isn't supported, peacing out");
            return;
        }

        Thread shutdownHook = new Thread(() -> {
            ExceptionLogger exceptionLoggerInstance = ExceptionLogger.getInstance();
            exceptionLoggerInstance.close();
            System.out.println(EXCEPTION_BUDDY_TAG + " Shutdown hook complete. We out!");
        });
        shutdownHook.setName("eb-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
}
