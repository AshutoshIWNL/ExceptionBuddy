package com.asm.eb.it.targets;

import com.asm.eb.it.other.OtherExceptionGenerator;

import java.lang.management.ManagementFactory;

public class ExceptionBuddyTargetApp {

    private static final String MODE_STARTUP = "startup";
    private static final String MODE_FILTER = "filter";
    private static final String MODE_RUNTIME = "runtime";

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : MODE_STARTUP;
        System.out.println("PID=" + getPid());
        System.out.flush();

        if (MODE_FILTER.equals(mode)) {
            generateFilterEvents();
            return;
        }

        if (MODE_RUNTIME.equals(mode)) {
            long deadline = System.currentTimeMillis() + 12000L;
            while (System.currentTimeMillis() < deadline) {
                generateRuntimeEvent();
                Thread.sleep(200L);
            }
            return;
        }

        generateStartupEvent();
    }

    private static void generateStartupEvent() {
        try {
            throw new IllegalStateException("EB_STARTUP_EXCEPTION");
        } catch (IllegalStateException ignored) {
            // no-op
        }
    }

    private static void generateFilterEvents() {
        FilterHitGenerator.throwHit();
        OtherExceptionGenerator.throwMiss();
    }

    private static void generateRuntimeEvent() {
        try {
            throw new RuntimeException("EB_RUNTIME_ATTACH");
        } catch (RuntimeException ignored) {
            // no-op
        }
    }

    private static String getPid() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        int separator = runtimeName.indexOf('@');
        return separator > 0 ? runtimeName.substring(0, separator) : runtimeName;
    }
}
