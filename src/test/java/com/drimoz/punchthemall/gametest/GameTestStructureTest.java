package com.drimoz.punchthemall.gametest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps the game tests' structure template in sync with the shape they expect.
 *
 * <p>{@code TransformationGameTests} needs a template to define its test area, and Minecraft only
 * reads templates shipped in a mod as binary {@code .nbt} — the readable {@code .snbt} form is
 * loaded from a dev directory, not from resources. Rather than commit an opaque binary nobody can
 * review or regenerate, the template is described here in code, written on every run, and checked.
 * The file stays generated and reproducible, and the description of the test area lives somewhere a
 * reader can find it.</p>
 *
 * <p>It is deliberately empty: the tests place every block they care about, so a template carrying
 * scenery would only be a second place to look when one of them fails.</p>
 */
class GameTestStructureTest {

    /** 1.21.1. Only used so the template is not treated as needing a data fix. */
    private static final int DATA_VERSION = 3955;

    private static final int SIZE_X = 5;
    private static final int SIZE_Y = 4;
    private static final int SIZE_Z = 5;

    private static final Path TEMPLATE =
            // 1.20.1 reads game test templates from "structures"; 1.21 renamed the folder to "structure".
            Path.of("src", "main", "resources", "data", "pta", "structures", "pta_platform.nbt");

    @Test
    @DisplayName("the game test template is present and matches its description here")
    void templateIsGenerated() throws IOException {
        CompoundTag expected = describeTemplate();

        // Only written when it would actually change. Rewriting identical bytes on every run would
        // invalidate Gradle's up-to-date check on processResources and drag the build behind it.
        if (!matchesOnDisk(expected)) {
            Files.createDirectories(TEMPLATE.getParent());
            // 1.20.1 NbtIo works in streams and has no NbtAccounter argument.
            try (OutputStream out = Files.newOutputStream(TEMPLATE)) { NbtIo.writeCompressed(expected, out); }
        }

        assertTrue(Files.exists(TEMPLATE), "the template should exist at " + TEMPLATE.toAbsolutePath());
        assertEquals(expected, readTemplate());
    }

    private static boolean matchesOnDisk(CompoundTag expected) {
        if (!Files.exists(TEMPLATE)) return false;
        try {
            return expected.equals(readTemplate());
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** An empty {@value #SIZE_X}x{@value #SIZE_Y}x{@value #SIZE_Z} box of air. */
    private static CompoundTag describeTemplate() {
        CompoundTag template = new CompoundTag();
        template.putInt("DataVersion", DATA_VERSION);
        template.put("size", intList(SIZE_X, SIZE_Y, SIZE_Z));

        // A palette entry is required even with no blocks placed against it.
        CompoundTag air = new CompoundTag();
        air.put("Name", StringTag.valueOf("minecraft:air"));
        ListTag palette = new ListTag();
        palette.add(air);
        template.put("palette", palette);

        template.put("blocks", new ListTag());
        template.put("entities", new ListTag());
        return template;
    }

    private static ListTag intList(int... values) {
        ListTag list = new ListTag();
        for (int value : values) list.add(IntTag.valueOf(value));
        return list;
    }

    private static CompoundTag readTemplate() throws IOException {
        try (InputStream in = Files.newInputStream(TEMPLATE)) {
            return NbtIo.readCompressed(in);
        }
    }
}
