package com.drimoz.punchthemall.core.network;

import com.drimoz.punchthemall.PunchThemAll;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → server notice that the player left-clicked nothing.
 *
 * <p>{@code Minecraft.startAttack} posts {@code PlayerInteractEvent.LeftClickEmpty} on the physical
 * client only, so a {@code left_click} interaction targeting air has no server-side event to hang
 * off. This carries no data on purpose: it is a bare signal, and the server re-derives the player,
 * hand, position and every config gate itself, so a spoofed or spammed packet can only ask for what
 * the player could already have triggered by swinging.</p>
 */
public record LeftClickEmptyPayload() implements CustomPacketPayload {

    public static final LeftClickEmptyPayload INSTANCE = new LeftClickEmptyPayload();

    public static final Type<LeftClickEmptyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PunchThemAll.MOD_ID, "left_click_empty"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LeftClickEmptyPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
