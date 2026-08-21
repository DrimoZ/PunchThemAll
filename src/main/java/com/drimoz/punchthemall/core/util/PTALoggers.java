package com.drimoz.punchthemall.core.util;

import com.drimoz.punchthemall.PunchThemAll;

import org.slf4j.Logger;

public class PTALoggers {
    private static final Logger LOGGER = PunchThemAll.LOGGER;

    /**
     * Silences problem reports on the current thread.
     *
     * <p>Interactions are read twice on a reload: once as resources load, and again once tags are
     * bound, because tag contents do not exist during the first pass. The first pass therefore
     * reports every tag selector as unresolvable, and every one of those reports is wrong by the
     * time loading finishes. Quiet covers that pass only, so the log carries the problems the second
     * pass actually found and nothing else.</p>
     *
     * <p>Thread-local because the client and the integrated server load in the same JVM.</p>
     */
    private static final ThreadLocal<Boolean> QUIET = ThreadLocal.withInitial(() -> false);

    public static void runQuietly(Runnable action) {
        QUIET.set(true);
        try {
            action.run();
        } finally {
            QUIET.set(false);
        }
    }

    public static void infoRegisteredModule(String moduleName) {
        LOGGER.info(PunchThemAll.MOD_NAME + " - Successfully registered : {}", moduleName);
    }

    public static void infoModCompleted() {
        LOGGER.info(PunchThemAll.MOD_NAME + " - Successfully registered");
    }

    public static void error(String message) {
        if (QUIET.get()) return;
        LOGGER.error(PunchThemAll.MOD_NAME + " - {}", message);
    }

    public static void warn(String message) {
        if (QUIET.get()) return;
        LOGGER.warn(PunchThemAll.MOD_NAME + " - {}", message);
    }

    public static void info(String message) {
        LOGGER.info(PunchThemAll.MOD_NAME + " - {}", message);
    }
}
