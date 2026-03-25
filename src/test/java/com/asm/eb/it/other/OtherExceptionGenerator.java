package com.asm.eb.it.other;

public final class OtherExceptionGenerator {

    private OtherExceptionGenerator() {
    }

    public static void throwMiss() {
        try {
            throw new IllegalStateException("EB_FILTER_MISS");
        } catch (IllegalStateException ignored) {
            // no-op
        }
    }
}
