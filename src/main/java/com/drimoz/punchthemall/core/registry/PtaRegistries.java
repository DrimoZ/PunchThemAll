package com.drimoz.punchthemall.core.registry;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.codec.InteractionSpec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Registry keys owned by PunchThemAll.
 *
 * <p>{@link #INTERACTION} is a <b>datapack registry</b> of raw {@link InteractionSpec}s, loaded from
 * {@code data/<namespace>/pta/interaction/*.json}. Declaring it with a network codec means vanilla
 * synchronises it to clients automatically — so JEI/EMI are correct on dedicated servers with no
 * custom networking. The specs are resolved into the runtime {@link InteractionRegistry} on both
 * sides once the registry is available.</p>
 */
public final class PtaRegistries {

    public static final ResourceKey<Registry<InteractionSpec>> INTERACTION =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(PunchThemAll.MOD_ID, "interaction"));

    private PtaRegistries() {}
}
