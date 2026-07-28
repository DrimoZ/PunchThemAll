package com.drimoz.punchthemall.core.model.classes;

import com.drimoz.punchthemall.McBootstrap;
import com.drimoz.punchthemall.core.model.enums.PtaHandEnum;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The invariants the model classes enforce in their constructors. These are what let the rest of the
 * code skip null checks, so they are worth pinning down.
 */
class PtaModelInvariantsTest {

    @BeforeAll
    static void bootstrap() {
        McBootstrap.ensure();
    }

    private static Set<Item> items(Item... items) {
        return new HashSet<>(Set.of(items));
    }

    @Nested
    @DisplayName("PtaHand")
    class Hand {

        @Test
        @DisplayName("an empty hand reports no hand and no constraints")
        void emptyHand() {
            PtaHand hand = PtaHand.createEmpty(PtaHandEnum.MAIN_HAND);

            assertTrue(hand.isEmpty());
            assertNull(hand.getHand(), "an empty hand has no hand to report");
            assertFalse(hand.hasNbtWhiteList());
            assertFalse(hand.hasNbtBlackList());
            assertFalse(hand.hasNbtPredicates());
            assertFalse(hand.isConsumable());
            assertFalse(hand.isDamageable());
            assertEquals(0, hand.getChance());
        }

        @Test
        @DisplayName("an item set brings the hand and its constraints to life")
        void populatedHand() {
            CompoundTag whitelist = new CompoundTag();
            whitelist.putInt("Damage", 0);

            PtaHand hand = PtaHand.create(PtaHandEnum.OFF_HAND, items(Items.DIAMOND_PICKAXE),
                    whitelist, new CompoundTag(), List.of(), 0.5, true, false);

            assertFalse(hand.isEmpty());
            assertEquals(PtaHandEnum.OFF_HAND, hand.getHand());
            assertTrue(hand.useOffHand());
            assertFalse(hand.useMainHand());
            assertTrue(hand.hasNbtWhiteList());
            assertTrue(hand.isDamageable());
            assertFalse(hand.isConsumable());
        }

        @Test
        @DisplayName("chance is clamped to [0, 1]")
        void chanceClamped() {
            assertEquals(1.0, PtaHand.create(PtaHandEnum.ANY_HAND, items(Items.STICK),
                    null, null, List.of(), 4, false, true).getChance());
            assertEquals(0.0, PtaHand.create(PtaHandEnum.ANY_HAND, items(Items.STICK),
                    null, null, List.of(), -1, false, true).getChance());
        }

        @Test
        @DisplayName("a zero chance disables consumption entirely")
        void zeroChanceDisablesConsumption() {
            PtaHand hand = PtaHand.create(PtaHandEnum.ANY_HAND, items(Items.STICK),
                    null, null, List.of(), 0, true, true);

            assertFalse(hand.isConsumable());
            assertFalse(hand.isDamageable());
            assertFalse(hand.shouldConsume(RandomSource.create(1L)));
        }

        @Test
        @DisplayName("a certain consumption always fires, an empty hand never does")
        void shouldConsume() {
            PtaHand always = PtaHand.create(PtaHandEnum.ANY_HAND, items(Items.STICK),
                    null, null, List.of(), 1, false, true);
            RandomSource random = RandomSource.create(3L);

            for (int i = 0; i < 200; i++) {
                assertTrue(always.shouldConsume(random));
            }
            assertFalse(PtaHand.createEmpty(PtaHandEnum.ANY_HAND).shouldConsume(random));
        }

        @Test
        @DisplayName("nulls become empty collections")
        void nullsNormalised() {
            PtaHand hand = PtaHand.create(PtaHandEnum.ANY_HAND, items(Items.STICK), null, null, null, 1, false, true);

            assertNotNull(hand.getNbtWhiteList());
            assertNotNull(hand.getNbtBlackList());
            assertNotNull(hand.getNbtPredicates());
        }

        @Test
        @DisplayName("getStacks mirrors the item set")
        void stacks() {
            assertEquals(2, PtaHand.create(PtaHandEnum.ANY_HAND, items(Items.STICK, Items.DIAMOND),
                    null, null, List.of(), 0, false, false).getStacks().size());
        }
    }

    @Nested
    @DisplayName("PtaBlock")
    class Target {

        @Test
        @DisplayName("air is the absence of both sets")
        void air() {
            PtaBlock air = PtaBlock.createAir();

            assertTrue(air.isAir());
            assertFalse(air.isBlock());
            assertFalse(air.isFluid());
            assertTrue(air.getBlockStacks().isEmpty());
            assertNull(air.getFluid());
        }

        @Test
        @DisplayName("a block target is a block and nothing else")
        void block() {
            PtaBlock target = PtaBlock.createBlock(new HashSet<>(Set.of(Blocks.STONE)),
                    new HashSet<>(), new HashSet<>(), new CompoundTag(), new CompoundTag());

            assertTrue(target.isBlock());
            assertFalse(target.isAir());
            assertFalse(target.isFluid());
            assertTrue(target.isBlockFromSet(Blocks.STONE));
            assertFalse(target.isBlockFromSet(Blocks.DIRT));
            assertFalse(target.isFluidFromSet(Fluids.WATER));
            assertEquals(1, target.getBlockStacks().size());
        }

        @Test
        @DisplayName("a fluid target is a fluid and nothing else")
        void fluid() {
            PtaBlock target = PtaBlock.createFluid(new HashSet<>(Set.of(Fluids.WATER)),
                    new HashSet<>(), new HashSet<>(), new CompoundTag(), new CompoundTag());

            assertTrue(target.isFluid());
            assertFalse(target.isBlock());
            assertEquals(Fluids.WATER, target.getFluid());
            assertTrue(target.isFluidFromSet(Fluids.WATER));
            assertFalse(target.isBlockFromSet(Blocks.STONE));
        }

        @Test
        @DisplayName("a target cannot be both a block and a fluid")
        void cannotMix() {
            assertThrows(IllegalArgumentException.class, () -> new PtaBlock(
                    new HashSet<>(Set.of(Blocks.STONE)), new HashSet<>(Set.of(Fluids.WATER)),
                    null, null, null, null, List.of()) {});
        }

        @Test
        @DisplayName("state and NBT constraints are reported only when present")
        void constraintFlags() {
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("Level", 3);
            Set<PtaStateRecord<?>> states = new HashSet<>(Set.of(
                    new PtaStateRecord<>(BlockStateProperties.AXIS, "y")));

            PtaBlock bare = PtaBlock.createBlock(new HashSet<>(Set.of(Blocks.STONE)),
                    new HashSet<>(), new HashSet<>(), new CompoundTag(), new CompoundTag());
            assertFalse(bare.hasStateWhiteList());
            assertFalse(bare.hasNbtWhiteList());
            assertFalse(bare.hasNbtPredicates());

            PtaBlock constrained = PtaBlock.createBlock(new HashSet<>(Set.of(Blocks.OAK_LOG)),
                    states, new HashSet<>(), nbt, new CompoundTag(), List.of());
            assertTrue(constrained.hasStateWhiteList());
            assertTrue(constrained.hasNbtWhiteList());
            assertFalse(constrained.hasNbtBlackList());
        }
    }

    @Nested
    @DisplayName("PtaTransformation")
    class Transformation {

        @Test
        @DisplayName("chance 0 means no transformation at all")
        void noTransformation() {
            PtaTransformation none = PtaTransformation.createAir(0, null, null);

            assertFalse(none.hasTransformation());
            assertFalse(none.isAir());
            assertFalse(none.isBlock());
            assertFalse(none.isFluid());
            assertFalse(none.hasSound());
            assertFalse(none.hasParticles());
        }

        @Test
        @DisplayName("a chance turns the air form into 'break the block'")
        void breakForm() {
            PtaTransformation breaking = PtaTransformation.createAir(0.5, null, null);

            assertTrue(breaking.hasTransformation());
            assertTrue(breaking.isAir());
            assertNull(breaking.getBlock());
            assertNull(breaking.getFluid());
        }

        @Test
        @DisplayName("chance is clamped to [0, 1]")
        void chanceClamped() {
            assertEquals(1.0, PtaTransformation.createAir(9, null, null).getChance());
            assertEquals(0.0, PtaTransformation.createAir(-9, null, null).getChance());
        }

        @Test
        @DisplayName("a block form carries its state and NBT")
        void blockForm() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("id", "minecraft:furnace");

            PtaTransformation into = PtaTransformation.createBlock(1, Blocks.FURNACE,
                    new HashSet<>(), nbt, null, null);

            assertTrue(into.isBlock());
            assertEquals(Blocks.FURNACE, into.getBlock());
            assertTrue(into.hasNbtList());
            assertFalse(into.hasStateList());
        }

        @Test
        @DisplayName("a transformation cannot be both a block and a fluid")
        void cannotMix() {
            assertThrows(IllegalArgumentException.class, () -> new PtaTransformation(
                    1, Blocks.STONE, Fluids.WATER, null, null, null, null) {});
        }
    }

    @Nested
    @DisplayName("PtaExtras")
    class Extras {

        @Test
        @DisplayName("EMPTY carries nothing")
        void empty() {
            assertTrue(PtaExtras.EMPTY.conditions().isEmpty());
            assertFalse(PtaExtras.EMPTY.hasEffects());
            assertFalse(PtaExtras.EMPTY.hasSound());
            assertFalse(PtaExtras.EMPTY.hasParticles());
        }

        @Test
        @DisplayName("nulls are normalised so callers never null-check")
        void nullsNormalised() {
            PtaExtras extras = new PtaExtras(null, null, null, null);

            assertNotNull(extras.conditions());
            assertNotNull(extras.effects());
            assertTrue(extras.conditions().isEmpty());
            assertFalse(extras.hasEffects());
        }
    }

    @Nested
    @DisplayName("PtaStateRecord")
    class StateRecord {

        @Test
        @DisplayName("a valid property value is accepted and resolvable")
        void validValue() {
            PtaStateRecord<?> record = new PtaStateRecord<>(BlockStateProperties.AXIS, "y");

            assertFalse(record.isCopyValue());
            assertEquals(net.minecraft.core.Direction.Axis.Y, record.getValue());
        }

        @Test
        @DisplayName("a value outside the property is rejected at construction")
        void invalidValue() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PtaStateRecord<>(BlockStateProperties.AXIS, "diagonal"));
        }

        @Test
        @DisplayName("the copy sentinel bypasses validation and has no direct value")
        void copySentinel() {
            PtaStateRecord<?> record = new PtaStateRecord<>(BlockStateProperties.AXIS, "copy_state_value");

            assertTrue(record.isCopyValue());
            assertThrows(UnsupportedOperationException.class, record::getValue);
        }
    }
}
