// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.hazard.HazardClass;
import com.hbm.items.ModDataComponents;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.tags.HbmItemTags;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.Level;

public final class ArmorUtil {

    public static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private static final String FILTER_ID = "filter_stack";

    private ArmorUtil() {}

    public static boolean checkForHazmat(LivingEntity entity) {
        return SuitProtections.protects(entity, SuitProtection.Kind.HAZMAT);
    }

    public static boolean checkForAsbestos(LivingEntity entity) {
        return SuitProtections.protects(entity, SuitProtection.Kind.ASBESTOS);
    }

    public static boolean checkForHaz2(LivingEntity entity) {
        return SuitProtections.protects(entity, SuitProtection.Kind.SEALED);
    }

    public static boolean checkForDigamma(LivingEntity entity) {
        return SuitProtections.protects(entity, SuitProtection.Kind.DIGAMMA);
    }

    public static boolean checkForDigamma2(Player player) {
        return SuitProtections.protects(player, SuitProtection.Kind.DIGAMMA2);
    }

    public static boolean checkForFaraday(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty() || !isFaradayArmor(armor)) return false;
        }
        return true;
    }

    public static boolean isFaradayArmor(ItemStack item) {
        return item.is(HbmItemTags.FARADAY_ARMOR) || HazmatRegistry.getCladding(item) > 0;
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
        if (EquipmentProtections.isMask(mask)) damageGasMaskFilter(mask, damage);
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
        ItemTooltipCallback.EVENT.invoker().getTooltip(filter, context, flag, lore);
        for (Component line : lore)
            adder.accept(Component.literal("  ").withStyle(ChatFormatting.YELLOW).append(line));
    }

    public static void addGasMaskBlacklist(ItemStack mask, Consumer<Component> adder) {
        HazardClass[] blacklist = EquipmentProtections.exclusionOrder(mask);
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
        int worn = Math.addExact(filter.getDamageValue(), damage);
        if (worn > filter.getMaxDamage()) removeGasMaskFilter(mask);
        else {
            filter.setDamageValue(worn);
            installGasMaskFilter(mask, filter);
        }
    }

    public static net.minecraft.world.InteractionResult installFilterFromHand(
            Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack filter = player.getItemInHand(hand);
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return net.minecraft.world.InteractionResult.PASS;
        boolean socketed = !EquipmentProtections.isMask(helmet);
        ItemStack target =
                socketed ? ArmorModHandler.pryMod(helmet, ArmorModHandler.HELMET_ONLY) : helmet;
        if (!EquipmentProtections.isMask(target)) return net.minecraft.world.InteractionResult.PASS;
        ItemStack current = getGasMaskFilter(target);
        installGasMaskFilter(target, filter.copyWithCount(1));
        if (socketed) ArmorModHandler.applyMod(helmet, target);
        if (current.isEmpty()) filter.shrink(1);
        else player.setItemInHand(hand, current);
        level.playSound(
                null,
                player.blockPosition(),
                com.hbm.sound.ModSounds.GASMASK_SCREW.get(),
                net.minecraft.sounds.SoundSource.PLAYERS,
                1F,
                1F);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    public static void installGasMaskFilter(ItemStack mask, ItemStack filter) {
        if (filter.isEmpty()) return;
        var encoded =
                ItemStack.CODEC
                        .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, filter.copyWithCount(1))
                        .getOrThrow();
        CustomData.update(
                ModDataComponents.PERSISTENT_DATA.get(), mask, tag -> tag.put(FILTER_ID, encoded));
    }

    public static ItemStack removeGasMaskFilter(ItemStack mask) {
        ItemStack filter = getGasMaskFilter(mask);
        CustomData.update(
                ModDataComponents.PERSISTENT_DATA.get(),
                mask,
                tag -> {
                    tag.remove(FILTER_ID);
                });
        return filter;
    }

    public static ItemStack getGasMaskFilter(net.minecraft.world.item.ItemInstance mask) {
        CustomData data =
                mask.getOrDefault(ModDataComponents.PERSISTENT_DATA.get(), CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (!tag.contains(FILTER_ID)) return ItemStack.EMPTY;
        return ItemStack.CODEC
                .parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.getCompoundOrEmpty(FILTER_ID))
                .getOrThrow();
    }
}
