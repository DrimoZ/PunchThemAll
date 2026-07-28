package com.drimoz.punchthemall.core.model.classes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The typed {@code nbt_predicates} form: dotted path, {@code []} list step, {@code where}, range. */
class PtaNbtPredicateTest {

    private static PtaNbtPredicate presence(String path) {
        return new PtaNbtPredicate(path, Optional.empty(), Optional.empty(), null);
    }

    private static PtaNbtPredicate range(String path, Integer min, Integer max) {
        return new PtaNbtPredicate(path, Optional.ofNullable(min), Optional.ofNullable(max), null);
    }

    /** An item view carrying one enchantment and some custom data, as ItemView would produce. */
    private static CompoundTag enchantedItem(String id, short level, int damage) {
        CompoundTag entry = new CompoundTag();
        entry.putString("id", id);
        entry.putShort("lvl", level);

        ListTag enchantments = new ListTag();
        enchantments.add(entry);

        CompoundTag view = new CompoundTag();
        view.put("Enchantments", enchantments);
        view.putInt("Damage", damage);
        return view;
    }

    @Test
    @DisplayName("a null root never matches")
    void nullRoot() {
        assertFalse(presence("Damage").matches(null));
    }

    @Test
    @DisplayName("presence-only: the path must resolve to something")
    void presenceOnly() {
        CompoundTag view = enchantedItem("minecraft:fortune", (short) 2, 10);

        assertTrue(presence("Damage").matches(view));
        assertTrue(presence("Enchantments").matches(view));
        assertFalse(presence("Missing").matches(view));
    }

    @Test
    @DisplayName("a range checks the numeric leaf, inclusively")
    void rangeOnLeaf() {
        CompoundTag view = enchantedItem("minecraft:fortune", (short) 2, 10);

        assertTrue(range("Damage", 10, 10).matches(view));
        assertTrue(range("Damage", 0, 20).matches(view));
        assertFalse(range("Damage", 11, 20).matches(view));
        assertFalse(range("Damage", 0, 9).matches(view));
    }

    @Test
    @DisplayName("an open-ended range bounds only the side that is given")
    void openEndedRange() {
        CompoundTag view = enchantedItem("minecraft:fortune", (short) 2, 10);

        assertTrue(range("Damage", 5, null).matches(view));
        assertTrue(range("Damage", null, 15).matches(view));
        assertFalse(range("Damage", 11, null).matches(view));
        assertFalse(range("Damage", null, 9).matches(view));
    }

    @Test
    @DisplayName("[] walks a list, and any element satisfying the predicate is enough")
    void listWildcard() {
        CompoundTag first = new CompoundTag();
        first.putString("id", "minecraft:unbreaking");
        first.putShort("lvl", (short) 1);
        CompoundTag second = new CompoundTag();
        second.putString("id", "minecraft:fortune");
        second.putShort("lvl", (short) 3);

        ListTag list = new ListTag();
        list.add(first);
        list.add(second);
        CompoundTag view = new CompoundTag();
        view.put("Enchantments", list);

        assertTrue(range("Enchantments[].lvl", 3, 5).matches(view));
        assertTrue(range("Enchantments[].lvl", 1, 1).matches(view));
        assertFalse(range("Enchantments[].lvl", 4, 5).matches(view));
    }

    @Test
    @DisplayName("where filters which list elements are considered")
    void whereFilter() {
        CompoundTag unbreaking = new CompoundTag();
        unbreaking.putString("id", "minecraft:unbreaking");
        unbreaking.putShort("lvl", (short) 5);
        CompoundTag fortune = new CompoundTag();
        fortune.putString("id", "minecraft:fortune");
        fortune.putShort("lvl", (short) 1);

        ListTag list = new ListTag();
        list.add(unbreaking);
        list.add(fortune);
        CompoundTag view = new CompoundTag();
        view.put("Enchantments", list);

        CompoundTag onlyFortune = new CompoundTag();
        onlyFortune.putString("id", "minecraft:fortune");

        // Fortune is level 1, so a "level 5" test must fail even though another enchantment is 5.
        PtaNbtPredicate fortuneAtLeastFive =
                new PtaNbtPredicate("Enchantments[].lvl", Optional.of(5), Optional.empty(), onlyFortune);
        assertFalse(fortuneAtLeastFive.matches(view));

        PtaNbtPredicate fortuneAtLeastOne =
                new PtaNbtPredicate("Enchantments[].lvl", Optional.of(1), Optional.empty(), onlyFortune);
        assertTrue(fortuneAtLeastOne.matches(view));
    }

    @Test
    @DisplayName("where compares numbers by value, so tag width does not matter")
    void whereComparesNumericallyAcrossWidths() {
        CompoundTag element = new CompoundTag();
        element.putShort("tier", (short) 2);
        element.putInt("value", 42);
        ListTag list = new ListTag();
        list.add(element);
        CompoundTag view = new CompoundTag();
        view.put("items", list);

        CompoundTag where = new CompoundTag();
        where.putInt("tier", 2); // int here, short on the element

        assertTrue(new PtaNbtPredicate("items[].value", Optional.of(42), Optional.of(42), where).matches(view));
    }

    @Test
    @DisplayName("a list step against a non-list resolves to nothing")
    void listStepOnNonList() {
        CompoundTag view = new CompoundTag();
        view.putString("Enchantments", "not a list");

        assertFalse(presence("Enchantments[].lvl").matches(view));
    }

    @Test
    @DisplayName("descending through a scalar resolves to nothing")
    void pathThroughScalar() {
        CompoundTag view = new CompoundTag();
        view.putInt("Damage", 3);

        assertFalse(presence("Damage.deeper").matches(view));
    }

    @Test
    @DisplayName("a presence predicate is satisfied by a non-numeric leaf, a range is not")
    void nonNumericLeaf() {
        CompoundTag inner = new CompoundTag();
        inner.putString("owner", "theo");
        CompoundTag view = new CompoundTag();
        view.put("custom", inner);

        assertTrue(presence("custom.owner").matches(view));
        assertFalse(range("custom.owner", 0, 10).matches(view));
    }

    @Test
    @DisplayName("an empty path resolves to the root itself")
    void emptyPath() {
        assertTrue(presence("").matches(new CompoundTag()));
    }
}
