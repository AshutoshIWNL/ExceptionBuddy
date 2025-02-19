package com.asm.eb.model;

import java.util.List;

/**
 * @author asmishra
 * @since 2/13/2025
 */
public class Configuration {
    private boolean useFilters;
    private List<String> filters;
    private String LogFilePath;
    private boolean classLoaderTracing;
    private boolean exceptionMonitoring;
    private String cnfSkipString;
    private boolean printJVMSysProps;
    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    public String getLogFilePath() {
        return LogFilePath;
    }

    public void setLogFilePath(String logFilePath) {
        LogFilePath = logFilePath;
    }

    public boolean isUseFilters() {
        return useFilters;
    }

    public void setUseFilters(boolean useFilters) {
        this.useFilters = useFilters;
    }

    public boolean isClassLoaderTracing() {
        return classLoaderTracing;
    }

    public void setClassLoaderTracing(boolean classLoaderTracing) {
        this.classLoaderTracing = classLoaderTracing;
    }

    public boolean isExceptionMonitoring() {
        return exceptionMonitoring;
    }

    public void setExceptionMonitoring(boolean exceptionMonitoring) {
        this.exceptionMonitoring = exceptionMonitoring;
    }

    public String getCnfSkipString() {
        return cnfSkipString;
    }

    public void setCnfSkipString(String cnfSkipString) {
        this.cnfSkipString = cnfSkipString;
    }

    public boolean isPrintJVMSysProps() {
        return printJVMSysProps;
    }

    public void setPrintJVMSysProps(boolean printJVMSysProps) {
        this.printJVMSysProps = printJVMSysProps;
    }
}
