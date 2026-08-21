package com.drimoz.punchthemall.client;

import com.drimoz.punchthemall.PTAConfig;
import com.drimoz.punchthemall.core.util.TranslationKeys;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Whether a recipe tooltip is showing its summary or its full breakdown.
 *
 * <p>An interaction can carry a dozen facts — several transformations, each with an operation, a
 * destination, a condition on that destination, and what it drops. Printing all of it always gives a
 * tooltip nobody reads; printing a summary only leaves a player unable to find out what the second
 * transformation actually does. So the detail sits behind a held key, and which key is the player's
 * choice.</p>
 *
 * <p>Client-side by construction: it reads the keyboard. Nothing on the server may call it.</p>
 */
public final class TooltipDetail {

    private TooltipDetail() {}

    private enum Mode {
        SHIFT, CONTROL, ALT, ALWAYS, NEVER;

        static Mode current() {
            String configured = PTAConfig.clientValueOrDefault(PTAConfig.CLIENT.tooltipDetailKey);
            try {
                return valueOf(configured.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return SHIFT;
            }
        }
    }

    /** Whether the player is asking for the long version right now. */
    public static boolean isExpanded() {
        return switch (Mode.current()) {
            case SHIFT -> held(InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT);
            case CONTROL -> held(InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL);
            case ALT -> held(InputConstants.KEY_LALT, InputConstants.KEY_RALT);
            case ALWAYS -> true;
            case NEVER -> false;
        };
    }

    /**
     * Whether either of a modifier key pair is down.
     *
     * <p>26.1 removed the static Screen.hasShiftDown and friends: modifier state now arrives on the
     * key event, which a tooltip does not have. This asks the window directly, which is what those
     * helpers did anyway.</p>
     */
    private static boolean held(int leftKey, int rightKey) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), leftKey)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), rightKey);
    }

    /**
     * The line telling the player there is more to see, or {@code null} when there is nothing to
     * tell them — because they are already seeing everything, or because they have turned the
     * expansion off and a hint would only be a nag.
     */
    public static Component hint() {
        Mode mode = Mode.current();
        if (mode == Mode.ALWAYS || mode == Mode.NEVER || isExpanded()) return null;

        return Component.translatable(TranslationKeys.INTERACTION_TOOLTIP_HINT, keyName(mode))
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
    }

    private static Component keyName(Mode mode) {
        String key = switch (mode) {
            case CONTROL -> TranslationKeys.INTERACTION_TOOLTIP_KEY_CONTROL;
            case ALT -> TranslationKeys.INTERACTION_TOOLTIP_KEY_ALT;
            default -> TranslationKeys.INTERACTION_TOOLTIP_KEY_SHIFT;
        };
        return Component.translatable(key).withStyle(ChatFormatting.GRAY);
    }
}
