package com.drimoz.punchthemall;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * One-time game setup shared by every test that touches registries, items or NBT.
 *
 * <p>{@link Bootstrap} is what fills {@code BuiltInRegistries}. Without it {@code Items.STICK} is
 * null and anything building an {@code ItemStack} fails in a way that reads like a mod bug rather
 * than a missing fixture.</p>
 *
 * <p>On NeoForge, Bootstrap alone is not enough: {@code FeatureFlags.<clinit>} asks
 * {@code FeatureFlagLoader} for the flags mods declare, which reads {@code LoadingModList.get()} —
 * null outside the loader, so class initialisation of {@code Blocks} (and therefore {@code Items})
 * fails before any test runs. Publishing an empty mod list first is enough: no mod files means no
 * modded flags to scan. It has to be reflective because {@code net.neoforged.fml.loading} is a
 * runtime-only package, absent from the compile classpath.</p>
 *
 * <p>Two things are deliberately still absent. <b>Tags</b> need a running server, so tests cover
 * id-based matching only — which is also why production code resolves tags on
 * {@code TagsUpdatedEvent} rather than at parse time. <b>The config</b> cannot be bound from outside
 * the loader either, so config-reading code goes through {@link PTAConfig#valueOrDefault} and sees
 * the declared defaults here.</p>
 */
public final class McBootstrap {

    private static boolean done;

    private McBootstrap() {}

    public static synchronized void ensure() {
        if (done) return;

        publishEmptyModList();

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        done = true;
    }

    private static void publishEmptyModList() {
        try {
            Class<?> loadingModList = Class.forName("net.neoforged.fml.loading.LoadingModList");
            // of(plugins, modFiles, sortedList, modLoadingIssues, modDependencies) — every argument is
            // a collection, so empty ones are correct whatever the parameter order turns out to be.
            Method of = loadingModList.getMethod("of", List.class, List.class, List.class, List.class, Map.class);
            of.invoke(null, List.of(), List.of(), List.of(), List.of(), Map.of());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Could not publish an empty FML mod list; NeoForge's internals have moved and the "
                            + "test bootstrap needs updating.", e);
        }
    }
}
