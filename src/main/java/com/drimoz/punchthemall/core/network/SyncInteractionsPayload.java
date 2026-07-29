package com.drimoz.punchthemall.core.network;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.codec.InteractionSpec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server → client sync of the loaded interaction specs, in batches.
 *
 * <p>Sent on {@code OnDatapackSyncEvent}, so clients get the set on join and again after every
 * {@code /reload}. The unresolved {@link InteractionSpec}s travel rather than the runtime model:
 * they already have a codec, and resolving them client-side keeps registry lookups (enchantments,
 * effects, tags) on the receiving side's own registries.</p>
 *
 * <p>The set is split into batches because the specs are encoded as NBT and the vanilla reader caps
 * a single tag at 2 MiB. One payload for a large pack would sail past that and drop the client at
 * join with a decode error rather than a diagnosable message. {@code first} tells the receiver to
 * start a fresh set and {@code last} tells it the set is complete, so a rebuild only ever happens on
 * a whole, consistent batch series.</p>
 */
public record SyncInteractionsPayload(boolean first, boolean last, Map<Identifier, InteractionSpec> specs)
        implements CustomPacketPayload {

    /**
     * Interactions per payload. Deliberately well under the 2 MiB tag ceiling: even a spec carrying
     * several long SNBT strings stays in the low kilobytes, so 64 leaves roughly two orders of
     * magnitude of headroom.
     */
    public static final int BATCH_SIZE = 64;

    public static final Type<SyncInteractionsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(PunchThemAll.MOD_ID, "sync_interactions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncInteractionsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    SyncInteractionsPayload::first,
                    ByteBufCodecs.BOOL,
                    SyncInteractionsPayload::last,
                    ByteBufCodecs.map(
                            HashMap::new,
                            Identifier.STREAM_CODEC,
                            ByteBufCodecs.fromCodecWithRegistries(InteractionSpec.CODEC)
                    ),
                    SyncInteractionsPayload::specs,
                    SyncInteractionsPayload::new
            );

    /**
     * Split a full interaction set into the payloads to send, in order. An empty set still produces
     * one payload, so a client that had interactions and now should have none is told so.
     */
    public static List<SyncInteractionsPayload> split(Map<Identifier, InteractionSpec> specs) {
        List<SyncInteractionsPayload> payloads = new ArrayList<>();

        if (specs.isEmpty()) {
            payloads.add(new SyncInteractionsPayload(true, true, Map.of()));
            return payloads;
        }

        List<Map.Entry<Identifier, InteractionSpec>> entries = new ArrayList<>(specs.entrySet());
        for (int start = 0; start < entries.size(); start += BATCH_SIZE) {
            int end = Math.min(entries.size(), start + BATCH_SIZE);
            Map<Identifier, InteractionSpec> batch = new LinkedHashMap<>();
            entries.subList(start, end).forEach(entry -> batch.put(entry.getKey(), entry.getValue()));
            payloads.add(new SyncInteractionsPayload(start == 0, end == entries.size(), batch));
        }

        return payloads;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
