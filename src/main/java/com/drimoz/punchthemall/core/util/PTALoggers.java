package com.drimoz.punchthemall.core.util;

import com.drimoz.punchthemall.PunchThemAll;

import org.slf4j.Logger;

public class PTALoggers {
    private static final Logger LOGGER = PunchThemAll.LOGGER;

    public static void infoRegisteredModule(String moduleName) {
        LOGGER.info(PunchThemAll.MOD_NAME + " - Successfully registered : {}", moduleName);
    }

    public static void infoModCompleted() {
        LOGGER.info(PunchThemAll.MOD_NAME + " - Successfully registered");
    }

    public static void error(String message) {
        LOGGER.error(PunchThemAll.MOD_NAME + " - {}", message);
    }

    public static void info(String message) {
        LOGGER.info(PunchThemAll.MOD_NAME + " - {}", message);
    }
}
