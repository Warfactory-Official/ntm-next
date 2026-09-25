// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.items.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ArmorDamageHandler {

    private static final float DNS_EXPLOSION_MULT = 0.001F;

    private ArmorDamageHandler() {}

    public static float filterAttack(LivingEntity entity, DamageSource source, float amount) {
        if (amount <= 0F || !(entity instanceof Player player)) return amount;

        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)
                && is(player, EquipmentSlot.HEAD, ModItems.NOSSY_HAT.get())
                && amount <= ItemHat.DAMAGE_THRESHOLD) {
            player.level()
                    .playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            SoundEvents.ITEM_BREAK.value(),
                            SoundSource.PLAYERS,
                            5F,
                            1.0F + player.getRandom().nextFloat() * 0.5F);
            return 0F;
        }

        if (wearsAll(
                player,
                ModItems.EUPHEMIUM_HELMET.get(),
                ModItems.EUPHEMIUM_PLATE.get(),
                ModItems.EUPHEMIUM_LEGS.get(),
                ModItems.EUPHEMIUM_BOOTS.get())) {
            plink(player);
            return 0F;
        }

        if (ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.DNS)) {
            if (source.is(DamageTypeTags.IS_EXPLOSION)) return amount;
            plink(player);
            return 0F;
        }

        if (ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.TRENCHMASTER)) {
            if (source.is(DamageTypeTags.IS_EXPLOSION) && source.getEntity() == player) return 0F;
            if (player.getRandom().nextInt(3) == 0) {
                plink(player);
                return 0F;
            }
        }

        return amount;
    }

    public static float filterHurt(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof Player player)) return amount;
        if (ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.DNS)
                && source.is(DamageTypeTags.IS_EXPLOSION)) {
            amount *= DNS_EXPLOSION_MULT;
        }
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)
                && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.NO9.get())) {
            amount = Math.max(0F, amount - 0.5F);
        }
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)
                && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.NOSSY_HAT.get())) {
            amount = Math.max(0F, amount - ItemHat.DAMAGE_THRESHOLD);
        }
        return amount;
    }

    private static void plink(Player player) {
        HbmPlayerProps.plink(
                player,
                SoundEvents.ITEM_BREAK.value(),
                0.5F,
                1.0F + player.getRandom().nextFloat() * 0.5F);
    }

    private static boolean wearsAll(Player player, Item helmet, Item chest, Item legs, Item boots) {
        return is(player, EquipmentSlot.HEAD, helmet)
                && is(player, EquipmentSlot.CHEST, chest)
                && is(player, EquipmentSlot.LEGS, legs)
                && is(player, EquipmentSlot.FEET, boots);
    }

    private static boolean is(Player player, EquipmentSlot slot, Item item) {
        ItemStack stack = player.getItemBySlot(slot);
        return !stack.isEmpty() && stack.is(item);
    }
}
