package com.drimoz.punchthemall.core.network;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.codec.InteractionSpec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Server → client sync of the loaded interaction specs.
 *
 * <p>Sent on {@code OnDatapackSyncEvent}, so clients get the set on join and again after every
 * {@code /reload}. The unresolved {@link InteractionSpec}s travel rather than the runtime model:
 * they already have a codec, and resolving them client-side keeps registry lookups (enchantments,
 * effects, tags) on the receiving side's own registries.</p>
 */
public record SyncInteractionsPayload(Map<ResourceLocation, InteractionSpec> specs) implements CustomPacketPayload {

    public static final Type<SyncInteractionsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PunchThemAll.MOD_ID, "sync_interactions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncInteractionsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(
                            HashMap::new,
                            ResourceLocation.STREAM_CODEC,
                            ByteBufCodecs.fromCodecWithRegistries(InteractionSpec.CODEC)
                    ),
                    SyncInteractionsPayload::specs,
                    SyncInteractionsPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
