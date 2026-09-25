// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.hazard.HazardClass;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.armor.IGasMask;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.platform.Services;
import com.hbm.potion.HbmPotion;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class ArmorUtil {

    public static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private static final String[] FARADAY_METALS = {
        "chainmail",
        "iron",
        "silver",
        "gold",
        "platinum",
        "tin",
        "lead",
        "liquidator",
        "schrabidium",
        "euphemium",
        "steel",
        "cmb",
        "titanium",
        "alloy",
        "copper",
        "bronze",
        "electrum",
        "t45",
        "t51",
        "bj",
        "starmetal",
        "hazmat",
        "rubber",
        "hev",
        "ajr",
        "rpa",
        "spacesuit"
    };

    private static final String FILTER_ID = "hfrFilter";
    private static final String FILTER_DAMAGE = "hfrFilterDamage";

    private ArmorUtil() {}

    public static boolean checkForHazmat(LivingEntity entity) {
        return wears(entity, ModArmorItem.Suit.HAZMAT)
                || wears(entity, ModArmorItem.Suit.HAZMAT_RED)
                || wears(entity, ModArmorItem.Suit.HAZMAT_GREY)
                || wears(entity, ModArmorItem.Suit.SCHRABIDIUM)
                || checkForHaz2(entity)
                || entity.hasEffect(HbmPotion.mutation());
    }

    public static boolean checkForAsbestos(LivingEntity entity) {
        return wears(entity, ModArmorItem.Suit.ASBESTOS);
    }

    public static boolean checkForHaz2(LivingEntity entity) {
        return wears(entity, ModArmorItem.Suit.HAZMAT_PAA)
                || wears(entity, ModArmorItem.Suit.LIQUIDATOR)
                || wears(entity, ModArmorItem.Suit.EUPHEMIUM)
                || wears(entity, ModArmorItem.Suit.RPA)
                || wears(entity, ModArmorItem.Suit.DIGAMMA)
                || wears(entity, ModArmorItem.Suit.DNS);
    }

    public static boolean checkForDigamma(LivingEntity entity) {
        return wears(entity, ModArmorItem.Suit.DIGAMMA)
                || wears(entity, ModArmorItem.Suit.DNS)
                || entity.hasEffect(HbmPotion.stability());
    }

    public static boolean checkForDigamma2(Player player) {
        if (!wears(player, ModArmorItem.Suit.ROBES) || !player.hasEffect(HbmPotion.stability()))
            return false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            if (ArmorModHandler.hasMods(armor)
                    && !ArmorModHandler.pryMods(armor)[ArmorModHandler.CLADDING].is(
                            ModItems.CLADDING_IRON.get())) {
                return false;
            }
        }
        return player.getMaxHealth() < 3;
    }

    public static boolean checkForFaraday(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty() || !isFaradayArmor(armor)) return false;
        }
        return true;
    }

    public static boolean isFaradayArmor(ItemStack item) {
        String name = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
        for (String metal : FARADAY_METALS) if (name.contains(metal)) return true;
        return HazmatRegistry.getCladding(item) > 0;
    }

    private static boolean wears(LivingEntity entity, ModArmorItem.Suit suit) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!(entity.getItemBySlot(slot).getItem() instanceof ModArmorItem item)
                    || item.suit() != suit) return false;
        }
        return true;
    }

    public static void damageWholeSuit(LivingEntity entity, int amount) {
        for (EquipmentSlot slot :
                new EquipmentSlot[] {
                    EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
                }) {
            damageSuit(entity, slot, amount);
        }
    }

    public static void damageSuit(LivingEntity entity, EquipmentSlot slot, int amount) {
        ItemStack piece = entity.getItemBySlot(slot);
        if (piece.isEmpty() || drainSupplyForWear(entity, slot, amount)) return;

        piece.hurtAndBreak(amount, entity, slot);
    }

    public static boolean drainSupplyForWear(LivingEntity entity, EquipmentSlot slot, int amount) {
        return drainSupplyForWear(entity, entity.getItemBySlot(slot), amount);
    }

    public static boolean drainSupplyForWear(LivingEntity entity, ItemStack piece, int amount) {
        if (!(piece.getItem() instanceof ModArmorItem armor) || !armor.drainsOnWear()) return false;
        if (entity.level() instanceof ServerLevel level && !entity.hasInfiniteMaterials()) {
            armor.drainForWear(
                    piece, EnchantmentHelper.processDurabilityChange(level, piece, amount));
        }
        return true;
    }

    public static void damageGasMaskFilter(LivingEntity entity, int damage) {
        ItemStack mask = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (mask.getItem() instanceof IGasMask) damageGasMaskFilter(mask, damage);
    }

    public static void addGasMaskTooltip(
            ItemStack mask,
            Item.TooltipContext context,
            TooltipFlag flag,
            Consumer<Component> adder) {
        ItemStack filter = getGasMaskFilter(mask);
        if (filter.isEmpty()) {
            adder.accept(
                    Component.translatable("desc.item.gasMask.noFilterInstalled")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        adder.accept(
                Component.translatable("desc.item.gasMask.installedFilter")
                        .withStyle(ChatFormatting.GOLD));
        int max = filter.getMaxDamage();
        String append = max > 0 ? " (" + ((max - filter.getDamageValue()) * 100 / max) + "%)" : "";
        adder.accept(Component.literal("  ").append(filter.getHoverName()).append(append));

        List<Component> lore = new ArrayList<>();
        filter.getItem()
                .appendHoverText(
                        filter,
                        context,
                        filter.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT),
                        lore::add,
                        flag);
        Services.PLATFORM.itemTooltipEvent(filter, context, flag, lore);
        for (Component line : lore)
            adder.accept(Component.literal("  ").withStyle(ChatFormatting.YELLOW).append(line));
    }

    public static void addGasMaskBlacklist(IGasMask mask, Consumer<Component> adder) {
        HazardClass[] blacklist = mask.filterBlacklist();
        if (blacklist.length == 0) return;
        adder.accept(Component.translatable("hazard.neverProtects").withStyle(ChatFormatting.RED));
        for (HazardClass clazz : blacklist) {
            adder.accept(
                    Component.literal(" -")
                            .append(Component.translatable(clazz.lang))
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }

    public static boolean ejectGasMaskFilter(Player player, ItemStack mask) {
        if (!player.isShiftKeyDown()) return false;
        ItemStack filter = removeGasMaskFilter(mask);
        if (filter.isEmpty()) return false;
        player.getInventory().placeItemBackInInventory(filter);
        return true;
    }

    public static void damageGasMaskFilter(ItemStack mask, int damage) {
        ItemStack filter = getGasMaskFilter(mask);
        if (filter.isEmpty() || filter.getMaxDamage() == 0) return;
        filter.setDamageValue(filter.getDamageValue() + damage);
        if (filter.getDamageValue() > filter.getMaxDamage()) removeGasMaskFilter(mask);
        else installGasMaskFilter(mask, filter);
    }

    public static void installGasMaskFilter(ItemStack mask, ItemStack filter) {
        if (filter.isEmpty()) return;
        CustomData.update(
                ModDataComponents.PERSISTENT_DATA.get(),
                mask,
                tag -> {
                    tag.putString(
                            FILTER_ID, BuiltInRegistries.ITEM.getKey(filter.getItem()).toString());
                    tag.putInt(FILTER_DAMAGE, filter.getDamageValue());
                });
    }

    public static ItemStack removeGasMaskFilter(ItemStack mask) {
        ItemStack filter = getGasMaskFilter(mask);
        CustomData.update(
                ModDataComponents.PERSISTENT_DATA.get(),
                mask,
                tag -> {
                    tag.remove(FILTER_ID);
                    tag.remove(FILTER_DAMAGE);
                });
        return filter;
    }

    public static ItemStack getGasMaskFilter(ItemStack mask) {
        CustomData data =
                mask.getOrDefault(ModDataComponents.PERSISTENT_DATA.get(), CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (!tag.contains(FILTER_ID)) return ItemStack.EMPTY;
        return BuiltInRegistries.ITEM
                .getOptional(Identifier.parse(tag.getStringOr(FILTER_ID, "")))
                .map(
                        item -> {
                            ItemStack filter = new ItemStack(item);
                            filter.setDamageValue(tag.getIntOr(FILTER_DAMAGE, 0));
                            return filter;
                        })
                .orElse(ItemStack.EMPTY);
    }
}
