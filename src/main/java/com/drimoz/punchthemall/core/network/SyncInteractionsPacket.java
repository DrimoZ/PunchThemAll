package com.drimoz.punchthemall.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * S2C payload carrying the server's interaction registry as raw JSON keyed by id.
 *
 * <p>The client re-parses each entry with the shared {@code InteractionParser} against its own
 * (server-synchronised) registries and tags, so JEI mirrors the server exactly on dedicated
 * servers. The heavy client-side work lives in {@link ClientPacketHandler} to keep client-only
 * classes off the server.</p>
 */
public class SyncInteractionsPacket {

    private final Map<ResourceLocation, String> sources;

    public SyncInteractionsPacket(Map<ResourceLocation, String> sources) {
        this.sources = sources;
    }

    public Map<ResourceLocation, String> sources() {
        return sources;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(sources.size());
        for (Map.Entry<ResourceLocation, String> entry : sources.entrySet()) {
            buffer.writeResourceLocation(entry.getKey());
            buffer.writeUtf(entry.getValue(), Integer.MAX_VALUE);
        }
    }

    public static SyncInteractionsPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Map<ResourceLocation, String> sources = new HashMap<>(Math.max(16, size));
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            String json = buffer.readUtf(Integer.MAX_VALUE);
            sources.put(id, json);
        }
        return new SyncInteractionsPacket(sources);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.applySync(this))
        );
        context.setPacketHandled(true);
    }
}
