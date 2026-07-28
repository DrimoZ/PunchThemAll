package com.drimoz.punchthemall;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Boots Minecraft once, before JUnit touches any test class.
 *
 * <p>Per-class {@code @BeforeAll} is not enough. The whole suite shares one JVM, and
 * {@code BuiltInRegistries} can only be initialised after {@link McBootstrap}: any class that
 * touches it first — a Mockito mock of {@code Player}, a stray reference to {@code Items} — leaves it
 * permanently in the "Not bootstrapped" state, and every later test fails with an error that points
 * nowhere near the class that caused it. Doing this at session scope removes the ordering hazard
 * entirely.</p>
 *
 * <p>Registered through {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener}.</p>
 */
public class McBootstrapSessionListener implements LauncherSessionListener {

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        McBootstrap.ensure();
    }
}
