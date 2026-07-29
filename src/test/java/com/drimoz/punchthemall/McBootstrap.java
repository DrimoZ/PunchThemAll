package com.drimoz.punchthemall;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * One-time game setup shared by every test that touches registries, items or NBT.
 *
 * <p>{@link Bootstrap} is what fills {@code BuiltInRegistries}. Without it {@code Items.STICK} is
 * null and anything building an {@code ItemStack} fails in a way that reads like a mod bug rather
 * than a missing fixture.</p>
 *
 * <p>On 1.21.1 this class also had to publish an empty {@code LoadingModList} by reflection, and a
 * {@code LauncherSessionListener} had to run it before JUnit loaded any test class. Both are gone:
 * since 26.1 {@code SharedConstants} asks {@code FMLEnvironment} whether it is in production, which
 * needs a current {@code FMLLoader}, so there is no longer any way to reach a usable Minecraft from
 * a bare JVM. The build enables ModDevGradle's {@code unitTest} integration instead, which runs the
 * suite through FML — the supported path, and the reason this class is now four lines.</p>
 *
 * <p>Two things are deliberately still absent. <b>Tags</b> need a running server, so tests cover
 * id-based matching only — which is also why production code resolves tags on
 * {@code TagsUpdatedEvent} rather than at parse time. <b>The config</b> is not bound here either, so
 * config-reading code goes through {@link PTAConfig#valueOrDefault} and sees the declared defaults.</p>
 *
 * <p><b>Known gap on 26.1.</b> Anything that constructs an {@code ItemStack} still fails here with
 * "Components not bound yet": {@code ItemStack}'s constructor reads
 * {@code Holder.Reference#components()}, and 26.1 binds those lazily in
 * {@code ReloadableServerResources#loadResources} rather than in {@link Bootstrap}. Running the two
 * steps by hand —
 * {@code BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(provider).forEach(PendingComponents::apply)}
 * — gets further but then trips NeoForge's {@code CommonHooks.validateComponent}, because the
 * provider from {@code VanillaRegistries.createLookup()} yields lazy {@code HolderSet}s with no
 * {@code equals}. A real server's provider would not. The supported answer is
 * {@code net.neoforged:testframework}'s {@code EphemeralTestServerProvider}, which boots a throwaway
 * server for the classes that need real stacks; that is a deliberate decision, not a detail, so it
 * is left for its own change. See docs/porting/neoforge-26.1-plan.md §6.</p>
 */
public final class McBootstrap {

    private static boolean done;

    private McBootstrap() {}

    public static synchronized void ensure() {
        if (done) return;

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        done = true;
    }
}
