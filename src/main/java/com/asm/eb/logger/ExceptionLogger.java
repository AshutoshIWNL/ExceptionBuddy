package com.asm.eb.logger;

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
    private static ExceptionLogger instance;
    private PrintWriter writer;
    private List<String> filters;
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private final Lock lock = new ReentrantLock();

    private ExceptionLogger(String logFilePath, List<String> filters) {
        this.filters = filters;
        try {
            writer = new PrintWriter(new FileWriter(logFilePath, true), true);
        } catch (IOException e) {
            System.err.println("[ExceptionBuddy] Failed to initialize logger: " + e.getMessage());
        }
    }

    public static synchronized ExceptionLogger getInstance(String logFilePath, List<String> filters) {
        if (instance == null) {
            instance = new ExceptionLogger(logFilePath, filters);
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
        lock.lock();
        try {
            if (writer == null || !shouldLog(ex))
                return;

            writer.println(getTimestamp() + " [EXCEPTION] " + ex.getClass().getName() + ": " + ex.getMessage());
            for (StackTraceElement element : ex.getStackTrace()) {
                writer.println("\tat " + element);
            }
            writer.println();
            writer.flush();
        } finally {
            lock.unlock();
        }
    }

    public void logInfo(String message) {
        lock.lock();
        try {
            if (writer == null) return;
            writer.println(getTimestamp() + " [INFO] " + message);
            writer.flush();
        } finally {
            lock.unlock();
        }
    }

    public void logError(String message) {
        lock.lock();
        try {
            if (writer == null) return;
            writer.println(getTimestamp() + " [ERROR] " + message);
            writer.flush();
        } finally {
            lock.unlock();
        }
    }

    public void logClassLoading(String message) {
        lock.lock();
        try {
            if (writer == null) return;
            writer.println(getTimestamp() + " [CLT]\n" + message + "\n");
            writer.flush();
        } finally {
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
                if (element.getClassName().startsWith(filter)) {
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
}
