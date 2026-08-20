package com.drimoz.punchthemall;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * That the game booted at all.
 *
 * <p>Registry-backed tests fail in ways that point nowhere near the cause when the bootstrap did
 * not run, so one test asserts the fixture itself rather than any mod behaviour.</p>
 */
class BootstrapSmokeTest {

    @Test
    @DisplayName("the vanilla registries are filled")
    void registriesAreUp() {
        assertNotNull(Items.STICK);
        assertNotNull(Blocks.GRAVEL);
    }
}
