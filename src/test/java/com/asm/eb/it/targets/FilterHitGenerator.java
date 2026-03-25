package com.asm.eb.it.targets;

public final class FilterHitGenerator {

    private FilterHitGenerator() {
    }

    public static void throwHit() {
        try {
            throw new IllegalArgumentException("EB_FILTER_HIT");
        } catch (IllegalArgumentException ignored) {
            // no-op
        }
    }
}
