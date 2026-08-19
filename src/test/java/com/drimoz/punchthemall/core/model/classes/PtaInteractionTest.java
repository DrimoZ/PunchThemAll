package com.drimoz.punchthemall.core.model.classes;

import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.drimoz.punchthemall.core.model.records.PtaInteractionRecord;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The assembled interaction: its invariants, its identity, and the effects it can apply. */
class PtaInteractionTest {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("pta_test", "one");

    private static PtaInteraction interaction(ResourceLocation id, boolean hidden, int contentHash) {
        return new PtaInteraction(id, PtaTypeEnum.LEFT_CLICK, null, null,
                null, null, java.util.List.of(), 1.0D, PtaRewards.of(PtaPool.create(null)),
                null, null, null, hidden, contentHash);
    }

    @Nested
    @DisplayName("invariants")
    class Invariants {

        @Test
        @DisplayName("id, type and rewards are mandatory")
        void mandatoryFields() {
            PtaRewards rewards = PtaRewards.of(PtaPool.create(null));

            assertThrows(IllegalArgumentException.class, () -> new PtaInteraction(
                    null, PtaTypeEnum.LEFT_CLICK, null, null, null, null, null, rewards, null, null, null));
            assertThrows(IllegalArgumentException.class, () -> new PtaInteraction(
                    ID, null, null, null, null, null, null, rewards, null, null, null));
            assertThrows(IllegalArgumentException.class, () -> new PtaInteraction(
                    ID, PtaTypeEnum.LEFT_CLICK, null, null, null, null, null, null, null, null, null));
        }

        @Test
        @DisplayName("null collaborators become their inert forms")
        void nullsNormalised() {
            PtaInteraction interaction = interaction(ID, false, 0);

            assertNotNull(interaction.getHand());
            assertTrue(interaction.getHand().isEmpty());
            assertNotNull(interaction.getBlock());
            assertTrue(interaction.getBlock().isAir());
            assertNotNull(interaction.getTransformation());
            assertFalse(interaction.getTransformation().hasTransformation());
            assertNotNull(interaction.getExtras());
            assertTrue(interaction.getConditions().isEmpty());
            assertTrue(interaction.getBiomeWhitelist().isEmpty());
        }

        @Test
        @DisplayName("an air target silently drops any transformation")
        void airDropsTransformation() {
            // There is no block at the position to turn into anything, so keeping the transformation
            // would only mean acting on whatever happens to be under the player's feet.
            PtaInteraction interaction = new PtaInteraction(ID, PtaTypeEnum.LEFT_CLICK, null, null,
                    null, PtaBlock.createAir(),
                    PtaTransformation.createBlock(1, Blocks.STONE, new HashSet<>(), null, null, null),
                    PtaRewards.of(PtaPool.create(null)), null, null, null);

            assertFalse(interaction.getTransformation().hasTransformation());
        }

        @Test
        @DisplayName("a whitelist and a blacklist are mutually exclusive at read time")
        void biomeListsAreExclusive() {
            Set<String> whitelist = new HashSet<>(Set.of("minecraft:overworld"));
            Set<String> blacklist = new HashSet<>(Set.of("minecraft:the_end"));

            PtaInteraction both = new PtaInteraction(ID, PtaTypeEnum.LEFT_CLICK, null, null, null, null, null,
                    PtaRewards.of(PtaPool.create(null)), whitelist, blacklist, null);

            assertFalse(both.hasBiomeWhiteList());
            assertFalse(both.hasBiomeBlackList());
        }

        @Test
        @DisplayName("costs are reported only when configured")
        void costFlags() {
            assertFalse(interaction(ID, false, 0).hasHurtPlayer());
            assertFalse(interaction(ID, false, 0).hasConsumeFood());

            PtaInteraction hurting = new PtaInteraction(ID, PtaTypeEnum.LEFT_CLICK,
                    new PtaInteractionRecord(1, 1, 1), null, null, null, null,
                    PtaRewards.of(PtaPool.create(null)), null, null, null);
            assertTrue(hurting.hasHurtPlayer());
            assertFalse(hurting.hasConsumeFood());
        }
    }

    @Nested
    @DisplayName("hidden")
    class Hidden {

        @Test
        @DisplayName("defaults to visible")
        void defaultsToVisible() {
            assertFalse(new PtaInteraction(ID, PtaTypeEnum.LEFT_CLICK, null, null, null, null, null,
                    PtaRewards.of(PtaPool.create(null)), null, null, null).isHidden());
        }

        @Test
        @DisplayName("is carried through untouched")
        void carried() {
            assertTrue(interaction(ID, true, 0).isHidden());
            assertFalse(interaction(ID, false, 0).isHidden());
        }
    }

    @Nested
    @DisplayName("identity")
    class Identity {

        @Test
        @DisplayName("same id and same source compare equal")
        void equalWhenUnchanged() {
            // This is what lets the recipe viewers tell "reloaded unchanged" from "edited". Under
            // identity equality every reload looked like a brand-new set, so JEI hid and re-added the
            // whole category each time and its permanent hidden set grew without bound.
            assertEquals(interaction(ID, false, 1234), interaction(ID, false, 1234));
            assertEquals(interaction(ID, false, 1234).hashCode(), interaction(ID, false, 1234).hashCode());
        }

        @Test
        @DisplayName("a different source makes it a different recipe")
        void differsWhenEdited() {
            assertNotEquals(interaction(ID, false, 1234), interaction(ID, false, 5678));
        }

        @Test
        @DisplayName("a different id makes it a different recipe")
        void differsById() {
            assertNotEquals(interaction(ID, false, 1234),
                    interaction(ResourceLocation.fromNamespaceAndPath("pta_test", "two"), false, 1234));
        }

        @Test
        @DisplayName("equality survives a round trip through a collection")
        void usableAsACollectionKey() {
            assertTrue(List.of(interaction(ID, false, 42)).contains(interaction(ID, false, 42)));
            assertFalse(List.of(interaction(ID, false, 42)).contains(interaction(ID, false, 43)));
        }

        @Test
        @DisplayName("nothing equals a non-interaction")
        void notEqualToOtherTypes() {
            assertNotEquals(interaction(ID, false, 0), "not an interaction");
            assertNotEquals(null, interaction(ID, false, 0));
        }
    }

    @Nested
    @DisplayName("PtaEffect")
    class Effects {

        private Holder<MobEffect> haste() {
            return BuiltInRegistries.MOB_EFFECT
                    .getHolder(ResourceKey.create(Registries.MOB_EFFECT, ResourceLocation.parse("minecraft:haste")))
                    .orElseThrow();
        }

        @Test
        @DisplayName("duration, amplifier and chance are clamped to usable values")
        void clamping() {
            PtaEffect effect = new PtaEffect(haste(), -50, -3, 4);

            assertEquals(1, effect.duration(), "a zero-tick effect would never be visible");
            assertEquals(0, effect.amplifier());
            assertEquals(1.0, effect.chance());

            assertEquals(0.0, new PtaEffect(haste(), 100, 0, -1).chance());
        }

        @Test
        @DisplayName("a certain effect always applies and a zero-chance one does not")
        void chanceExtremes() {
            RandomSource random = RandomSource.create(11L);

            PtaEffect always = new PtaEffect(haste(), 100, 0, 1);
            PtaEffect never = new PtaEffect(haste(), 100, 0, 0);

            int applied = 0;
            for (int i = 0; i < 500; i++) {
                assertTrue(always.shouldApply(random));
                if (never.shouldApply(random)) applied++;
            }
            assertEquals(0, applied);
        }

        @Test
        @DisplayName("a null effect never applies")
        void nullEffectNeverApplies() {
            assertFalse(new PtaEffect(null, 100, 0, 1).shouldApply(RandomSource.create(1L)));
        }

        @Test
        @DisplayName("the instance carries the configured duration and amplifier")
        void toInstance() {
            var instance = new PtaEffect(haste(), 240, 2, 1).toInstance();

            assertEquals(240, instance.getDuration());
            assertEquals(2, instance.getAmplifier());
        }
    }
}
