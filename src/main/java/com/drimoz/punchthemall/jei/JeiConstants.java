package com.drimoz.punchthemall.jei;

import com.drimoz.punchthemall.PunchThemAll;
import net.minecraft.resources.ResourceLocation;

public class JeiConstants {
        public static final ResourceLocation JEI_TEXTURE = new ResourceLocation(PunchThemAll.MOD_ID, "textures/gui/jei.png");
        public static final ResourceLocation JEI_ICON_TEXTURE = new ResourceLocation(PunchThemAll.MOD_ID, "textures/gui/punch.png");
        public static final ResourceLocation UID = new ResourceLocation(PunchThemAll.MOD_ID, "interactions");

        public static final int WIDTH = 162;
        public static final int HEIGHT_START = 44;
        public static final int X_MOUSE_ICON = 0;
        public static final int Y_MOUSE_ICON = HEIGHT_START - 18 * 2 - 3 - 5;
        public static final int X_SNEAK_ICON = 0;
        public static final int Y_SNEAK_ICON = HEIGHT_START - 18 - 5;
        public static final int X_HAND_ICON = WIDTH - 18 * 6 - 5;
        public static final int Y_HAND_ICON = Y_MOUSE_ICON;
        public static final int X_ARROW = WIDTH - 18 * 4 - 4;
        public static final int Y_ARROW = Y_MOUSE_ICON + 4;
        public static final int X_HAND_ITEM = WIDTH - 18 * 5 - 5;
        public static final int Y_HAND_ITEM = Y_MOUSE_ICON;
        public static final int X_BLOCK = WIDTH - 18 * 4;
        public static final int Y_BLOCK = Y_SNEAK_ICON;
        public static final int X_TRANSFORMATION = WIDTH - 18;
        public static final int Y_TRANSFORMATION = Y_SNEAK_ICON;
        public static final int X_BIOMES = X_TRANSFORMATION;
        public static final int Y_BIOMES = Y_MOUSE_ICON;
}