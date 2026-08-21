package com.drimoz.punchthemall.core.network;

import com.drimoz.punchthemall.core.event.PlayerInteractionHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client to server notice that the player left-clicked nothing.
 *
 * <p>Minecraft.startAttack posts PlayerInteractEvent.LeftClickEmpty on the physical client only, so
 * a {@code left_click} interaction targeting air has no server-side event to hang off. This branch
 * listened for that event and then returned on the first line of the handler, which checks for the
 * client side — so every air-targeting left click has been dead since the type existed.</p>
 *
 * <p>It carries no data on purpose: it is a bare signal, and the server re-derives the player, the
 * hand, the position and every config gate itself. A spoofed or spammed packet can only ask for what
 * the player could already have triggered by swinging.</p>
 */
public record LeftClickEmptyPacket() {

    public static final LeftClickEmptyPacket INSTANCE = new LeftClickEmptyPacket();

    public static void encode(LeftClickEmptyPacket packet, FriendlyByteBuf buffer) {
        // Nothing to write. The signal is the packet.
    }

    public static LeftClickEmptyPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(LeftClickEmptyPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> PlayerInteractionHandler.onLeftClickEmptyFromClient(ctx.getSender()));
        ctx.setPacketHandled(true);
    }
}
