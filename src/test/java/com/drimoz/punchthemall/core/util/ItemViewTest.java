package com.drimoz.punchthemall.core.util;

import com.drimoz.punchthemall.McBootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PTA's version-stable view of an item. The whole point is that a pack's {@code path} and SNBT
 * expressions read the same on 1.20.1 and 1.21, so the shape here is a contract, not an internal.
 */
class ItemViewTest {

    @BeforeAll
    static void bootstrap() {
        McBootstrap.ensure();
    }

    @Test
    @DisplayName("an empty or null stack views as an empty compound")
    void emptyStack() {
        assertTrue(ItemView.of(ItemStack.EMPTY).isEmpty());
        assertTrue(ItemView.of(null).isEmpty());
    }

    @Test
    @DisplayName("a plain item has no damage entry")
    void plainItem() {
        assertFalse(ItemView.of(new ItemStack(Items.DIAMOND)).contains("Damage"));
    }

    @Test
    @DisplayName("a damageable item exposes Damage, even at zero")
    void damageIsExposed() {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        assertEquals(0, ItemView.of(pickaxe).getInt("Damage"));

        pickaxe.setDamageValue(42);
        assertEquals(42, ItemView.of(pickaxe).getInt("Damage"));
    }

    @Test
    @DisplayName("modded NBT surfaces under `custom`")
    void customDataIsExposed() {
        CompoundTag custom = new CompoundTag();
        custom.putInt("tier", 3);
        custom.putString("owner", "theo");

        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));

        CompoundTag view = ItemView.of(stack);

        assertTrue(view.contains("custom"));
        assertEquals(3, view.getCompound("custom").getInt("tier"));
        assertEquals("theo", view.getCompound("custom").getString("owner"));
    }

    @Test
    @DisplayName("empty custom data does not create a `custom` key")
    void emptyCustomDataOmitted() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));

        assertFalse(ItemView.of(stack).contains("custom"));
    }

    @Test
    @DisplayName("an unenchanted item has no Enchantments key")
    void noEnchantmentsKeyWhenUnenchanted() {
        assertFalse(ItemView.of(new ItemStack(Items.DIAMOND_PICKAXE)).contains("Enchantments"));
    }

    @Test
    @DisplayName("applyTo writes Damage back onto the stack")
    void applyDamage() {
        CompoundTag view = new CompoundTag();
        view.putInt("Damage", 12);

        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemView.applyTo(stack, view);

        assertEquals(12, stack.getDamageValue());
    }

    @Test
    @DisplayName("applyTo stores `custom` in the custom_data component")
    void applyCustom() {
        CompoundTag custom = new CompoundTag();
        custom.putInt("tier", 7);
        CompoundTag view = new CompoundTag();
        view.put("custom", custom);

        ItemStack stack = new ItemStack(Items.DIAMOND);
        ItemView.applyTo(stack, view);

        assertEquals(7, stack.get(DataComponents.CUSTOM_DATA).copyTag().getInt("tier"));
    }

    @Test
    @DisplayName("applyTo puts unknown keys in custom_data rather than dropping them")
    void applyUnknownKeys() {
        CompoundTag view = new CompoundTag();
        view.putString("SomeModKey", "value");

        ItemStack stack = new ItemStack(Items.DIAMOND);
        ItemView.applyTo(stack, view);

        assertEquals("value", stack.get(DataComponents.CUSTOM_DATA).copyTag().getString("SomeModKey"));
    }

    @Test
    @DisplayName("applyTo on an empty view leaves the stack alone")
    void applyEmptyView() {
        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemView.applyTo(stack, new CompoundTag());
        ItemView.applyTo(stack, null);

        assertEquals(0, stack.getDamageValue());
        assertFalse(stack.has(DataComponents.CUSTOM_DATA));
    }

    @Test
    @DisplayName("Damage and custom survive a write-then-read cycle")
    void roundTrip() {
        CompoundTag custom = new CompoundTag();
        custom.putInt("tier", 2);
        CompoundTag view = new CompoundTag();
        view.putInt("Damage", 5);
        view.put("custom", custom);

        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemView.applyTo(stack, view);

        CompoundTag readBack = ItemView.of(stack);
        assertEquals(5, readBack.getInt("Damage"));
        assertEquals(2, readBack.getCompound("custom").getInt("tier"));
    }

    @Test
    @DisplayName("an authored Enchantments list is ignored on write, not applied wrongly")
    void enchantmentsNotAppliedOnWrite() {
        // Enchanted drops need level-time registry access, which applyTo does not have. The contract
        // is that it is skipped — the failure mode to avoid is silently writing it into custom_data,
        // where it would look applied but do nothing.
        CompoundTag view = new CompoundTag();
        view.put("Enchantments", new net.minecraft.nbt.ListTag());

        ItemStack stack = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemView.applyTo(stack, view);

        assertFalse(stack.has(DataComponents.CUSTOM_DATA));
    }
}
