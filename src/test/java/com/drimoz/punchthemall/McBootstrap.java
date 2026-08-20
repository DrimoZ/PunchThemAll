package com.drimoz.punchthemall;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * One-time game setup shared by every test that touches registries, items or NBT.
 *
 * <p>{@link Bootstrap} is what fills the registries. Without it {@code Items.STICK} is null and
 * anything building an {@code ItemStack} fails in a way that reads like a mod bug rather than a
 * missing fixture.</p>
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

        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        done = true;
    }
}
