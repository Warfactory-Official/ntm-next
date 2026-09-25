// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.impl;

import com.hbm.config.GunVisualConfig;
import com.hbm.items.ICustomizable;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.GunTimers;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class ItemGunNI4NI extends ItemGunBaseNT implements ICustomizable {

    public static final String KEY_COIN_COUNT = "coincount";
    public static final String KEY_COIN_CHARGE = "coincharge";

    public ItemGunNI4NI(WeaponQuality quality, Item.Properties properties, GunConfig... cfg) {
        super(quality, properties, cfg);
    }

    public static void resetColors(ItemStack stack) {
        removeValue(stack, "colors");
    }

    public static void setColors(ItemStack stack, int dark, int light, int grip) {
        setValueIntArray(stack, "colors", new int[] {dark, light, grip});
    }

    public static int[] getColors(ItemStack stack) {
        int[] colors = getValueIntArray(stack, "colors");
        return colors.length == 3 ? colors : null;
    }

    public static int getCoinCount(ItemStack stack) {
        return getValueInt(stack, KEY_COIN_COUNT);
    }

    public static void setCoinCount(ItemStack stack, int value) {
        setValueInt(stack, KEY_COIN_COUNT, value);
    }

    public static int getCoinCharge(ItemStack stack) {
        GunTimers entry = GunTimers.of(stack);
        return entry != null ? entry.coinCharge() : getValueInt(stack, KEY_COIN_CHARGE);
    }

    public static void setCoinCharge(ItemStack stack, int value) {
        GunTimers.entry(stack).setCoinCharge(value);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        int maxCoin = 4;
        if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NI4NI_NICKEL)) maxCoin += 2;
        if (XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NI4NI_DOUBLOONS))
            maxCoin += 2;

        if (getCoinCount(stack) < maxCoin) {
            setCoinCharge(stack, getCoinCharge(stack) + 1);

            if (getCoinCharge(stack) >= 80) {
                setCoinCharge(stack, 0);
                int newCount = getCoinCount(stack) + 1;
                setCoinCount(stack, newCount);

                if (slot == EquipmentSlot.MAINHAND) {
                    level.playSound(
                            null,
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            ModSounds.TECH_BOOP.get(),
                            SoundSource.PLAYERS,
                            1.0F,
                            1F + newCount / (float) maxCoin);
                }
            }
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        tooltip.accept(Component.translatable("desc.item.gunNI4NI.nowDonTGet"));
        tooltip.accept(
                Component.translatable("desc.item.gunNI4NI.i")
                        .withStyle(ChatFormatting.GRAY)
                        .append(
                                Component.translatable("desc.item.gunNI4NI.fuckingHate")
                                        .withStyle(ChatFormatting.RED))
                        .append(
                                Component.translatable("desc.item.gunNI4NI.thisGame")
                                        .withStyle(ChatFormatting.GRAY)));
        tooltip.accept(Component.translatable("desc.item.gunNI4NI.iDidnTDo"));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }

    @Override
    public void customize(ServerPlayer player, ItemStack stack, String... args) {
        if (args.length == 0) {
            resetColors(stack);
            player.sendSystemMessage(
                    Component.translatable("desc.item.gunNI4NI.colorsReset")
                            .withStyle(ChatFormatting.GREEN));
            return;
        }
        if (args.length != 3) {
            resetColors(stack);
            player.sendSystemMessage(
                    Component.translatable("desc.item.gunNI4NI.requiresThreeHexadecimal")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        try {
            int dark = Integer.parseInt(args[0], 16);
            int light = Integer.parseInt(args[1], 16);
            int grip = Integer.parseInt(args[2], 16);
            if (dark < 0
                    || dark > 0xFFFFFF
                    || light < 0
                    || light > 0xFFFFFF
                    || grip < 0
                    || grip > 0xFFFFFF) {
                player.sendSystemMessage(
                        Component.translatable("desc.item.gunNI4NI.colorsMustRangeFrom")
                                .withStyle(ChatFormatting.RED));
                return;
            }
            setColors(stack, dark, light, grip);
            player.sendSystemMessage(
                    Component.translatable("desc.item.gunNI4NI.colorsSet")
                            .withStyle(ChatFormatting.GREEN));
        } catch (Throwable ex) {
            player.sendSystemMessage(
                    Component.literal(ex.getLocalizedMessage()).withStyle(ChatFormatting.RED));
        }
    }
}
