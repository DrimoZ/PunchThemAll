package com.drimoz.punchthemall.core.network;

import com.drimoz.punchthemall.PunchThemAll;
import com.drimoz.punchthemall.core.registry.InteractionRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;

/**
 * Forge networking for PunchThemAll: a single channel used to push the server's interaction
 * registry to clients (S2C) so JEI is correct on dedicated servers.
 */
public final class PtaNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PunchThemAll.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private PtaNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(
                id++,
                SyncInteractionsPacket.class,
                SyncInteractionsPacket::encode,
                SyncInteractionsPacket::decode,
                SyncInteractionsPacket::handle
        );
    }

    /** Send the whole registry to one player (used on login). */
    public static void syncTo(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), currentPacket());
    }

    /** Broadcast the whole registry to every connected player (used after a reload). */
    public static void syncToAll() {
        CHANNEL.send(PacketDistributor.ALL.noArg(), currentPacket());
    }

    private static SyncInteractionsPacket currentPacket() {
        return new SyncInteractionsPacket(new HashMap<>(InteractionRegistry.getInstance().getSources()));
    }
}
