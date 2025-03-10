package com.asm.eb.logger;

import com.asm.eb.store.StatsStore;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author asmishra
 * @since 2/13/2025
 */
public class ExceptionLogger {
    private volatile static ExceptionLogger instance;
    private PrintWriter writer;
    private final List<String> filters; //Make it non-final if runtime config file changes has to be taken up
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final boolean monitorException;
    private final String cnfSkipString;
    private static final String defaultCnfSkipString = "java.lang.ClassLoader.loadClass";
    private static boolean isJdk9OrLater = Integer.parseInt(System.getProperty("java.version").split("\\.")[0]) >= 9;
    //To help avoid ClassCircularityError
    private static final ThreadLocal<Boolean> isInsideLogging = ThreadLocal.withInitial(() -> false);

    private final Lock lock = new ReentrantLock();

    private ExceptionLogger(String logFilePath, List<String> filters, boolean monitorException, String cnfSkipString) {
        this.filters = filters;
        this.monitorException = monitorException;
        this.cnfSkipString = cnfSkipString;
        try {
            writer = new PrintWriter(new FileWriter(logFilePath, true), true);
        } catch (IOException e) {
            System.err.println("[ExceptionBuddy] Failed to initialize logger: " + e.getMessage());
        }
    }

    public static synchronized ExceptionLogger getInstance(String logFilePath, List<String> filters, boolean monitorException, String cnfSkipString) {
        if (instance == null) {
            synchronized (ExceptionLogger.class) {
                if (instance == null) {
                    instance = new ExceptionLogger(logFilePath, filters, monitorException, cnfSkipString);
                }
            }
        }
        return instance;
    }

    public static synchronized ExceptionLogger getInstance() {
        if (instance == null) {
            System.err.println("[ExceptionBuddy] Logger not initialized with log file path. Call getInstance(logFilePath, filters) first.");
        }
        return instance;
    }

    public void logException(Throwable ex) {
        if (isInsideLogging.get()) {
            return;
        }
        lock.lock();
        isInsideLogging.set(true);
        try {
            if(!isJdk9OrLater) {
                if(ex instanceof ClassNotFoundException) {
                    if(shouldSkip(ex))
                        return;
                }
            }
            if(monitorException) {
                StatsStore.incrementExceptionCount();
                if (isCriticalJVMException(ex)) {
                    StatsStore.incrementCriticalExceptionCount(ex.getClass().getSimpleName());
                }
            }
            if (writer == null || !shouldLog(ex))
                return;

            writer.println(getTimestamp() + " [EXCEPTION] " + " [" + Thread.currentThread().getName() + "] " + ex.getClass().getName() + ": " + ex.getMessage());
            for (StackTraceElement element : ex.getStackTrace()) {
                writer.println("\tat " + element);
            }
            writer.println();
            writer.flush();
        } finally {
            isInsideLogging.set(false);
            lock.unlock();
        }
    }

    //Hacky solution to avoid misleading ClassNotFoundException (Dependent on user's knowledge on ClassNotFoundException stack frames in false positive scenarios)
    private boolean shouldSkip(Throwable ex) {
        if(cnfSkipString == null || !cnfSkipString.startsWith(defaultCnfSkipString))
            return false;
        for (StackTraceElement element : ex.getStackTrace()) {
            if(element.toString().equals(cnfSkipString))
                return true;
        }
        return false;
    }

    public void logInfo(String message) {
        lock.lock();
        try {
            if (writer == null) return;
            writer.println(getMessage(message, "INFO"));
            writer.flush();
        } finally {
            lock.unlock();
        }
    }

    public void logWarn(String message) {
        lock.lock();
        try {
            if (writer == null) return;
            writer.println(getMessage(message, "WARN"));
            writer.flush();
        } finally {
            lock.unlock();
        }
    }

    public void logError(String message) {
        lock.lock();
        try {
            if (writer == null) return;
            writer.println(getMessage(message, "ERROR"));
            writer.flush();
        } finally {
            lock.unlock();
        }
    }

    private String getMessage(String message, String level) {
        return getTimestamp() + " [" + level + "] " + "[" + Thread.currentThread().getName() + "] " + message;
    }

    public void logClassLoading(String message) {
        if (isInsideLogging.get()) {
            return;
        }
        lock.lock();
        isInsideLogging.set(true);
        try {
            if (writer == null) return;
            writer.println(getTimestamp() + " [CLT]" + " [" + Thread.currentThread().getName() + "]\n" + message + "\n");
            writer.flush();
        } finally {
            isInsideLogging.set(false);
            lock.unlock();
        }
    }

    private String getTimestamp() {
        return TIMESTAMP_FORMAT.format(new Date());
    }

    private boolean shouldLog(Throwable ex) {
        if(filters == null || filters.isEmpty())
            return true;
        for (StackTraceElement element : ex.getStackTrace()) {
            for (String filter : filters) {
                if (element.toString().startsWith(filter)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void close() {
        lock.lock();
        try {
            if (writer != null) {
                writer.close();
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean isCriticalJVMException(Throwable ex) {
        return (ex instanceof OutOfMemoryError ||
                ex instanceof StackOverflowError ||
                ex instanceof ExceptionInInitializerError ||
                ex instanceof NoClassDefFoundError ||
                ex instanceof InternalError ||
                ex instanceof UnknownError);
    }
}
