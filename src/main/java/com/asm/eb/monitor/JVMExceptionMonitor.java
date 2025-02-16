package com.asm.eb.monitor;

import com.asm.eb.logger.ExceptionLogger;
import com.asm.eb.store.StatsStore;

/**
 * Monitors JVM exceptions and periodically logs the exception count.
 * Uses a singleton pattern to ensure a single instance across the application.
 *
 * @author asmishra
 * @since 2/14/2025
 */
public class JVMExceptionMonitor implements Runnable {

    private volatile Thread thread = null;
    private final ExceptionLogger exceptionLogger;
    private static volatile JVMExceptionMonitor instance;


    private JVMExceptionMonitor(ExceptionLogger exceptionLogger) {
        this.exceptionLogger = exceptionLogger;
    }

    /**
     * Returns the singleton instance of JVMExceptionMonitor, initializing it if necessary.
     *
     * @param exceptionLogger The logger instance for recording exceptions.
     * @return The singleton instance of JVMExceptionMonitor.
     */
    public static JVMExceptionMonitor getInstance(ExceptionLogger exceptionLogger) {
        if (instance == null) {
            synchronized (JVMExceptionMonitor.class) {
                if (instance == null) {
                    instance = new JVMExceptionMonitor(exceptionLogger);
                }
            }
        }
        return instance;
    }

    /**
     * Retrieves the existing instance of JVMExceptionMonitor if initialized.
     * Logs a warning if accessed before initialization.
     *
     * @return The existing instance or null if not initialized.
     */
    public static JVMExceptionMonitor getInstance() {
        if(instance == null) {
            System.err.println("[ExceptionBuddy] JVMExceptionMonitor not initialized with logger. Call getInstance(exceptionLogger) first.");
        }
        return instance;
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            try {
                exceptionLogger.logInfo("Exceptions encountered so far: " + StatsStore.getExceptionCount());
                exceptionLogger.logInfo("Critical exception count by category: " + StatsStore.getCriticalExceptionStats());
                Thread.sleep(60 * 1000L);
            } catch (InterruptedException e) {
                exceptionLogger.logInfo("Interrupted. Exiting gracefully.");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Starts the JVM exception monitor if it's not already running.
     */
    public void execute() {
        if(thread != null && thread.isAlive()) {
            exceptionLogger.logWarn("JVMExceptionMonitor is already running");
            return;
        }
        exceptionLogger.logInfo("Starting JVM exception monitor");
        thread = new Thread(this, "eb-jvm-exception");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Stops the JVM exception monitor gracefully.
     */
    public void shutdown() {
        if(thread != null) {
            exceptionLogger.logInfo("Shutting down JVM exception monitor");
            thread.interrupt();
            try {
                thread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exceptionLogger.logWarn("Shutdown interrupted while waiting for JVMExceptionMonitor to exit.");
            }
            thread = null;
        }
    }

    /**
     * Checks if the monitor is currently inactive.
     *
     * @return true if the monitor is shut down, false otherwise.
     */
    public boolean isDown() {
        return thread == null;
    }

}
