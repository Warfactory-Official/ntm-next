// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemBattery;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class ItemPotatos extends ItemBattery {

    private static final int BASE_DELAY = 200, JITTER = 100;

    public ItemPotatos(Properties properties, long maxCharge, long chargeRate, long dischargeRate) {
        super(properties, maxCharge, chargeRate, dischargeRate);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (getCharge(stack) == 0) return;

        int timer = stack.getOrDefault(ModDataComponents.POTATO_TIMER.get(), 0);
        if (timer > 0) {
            stack.set(ModDataComponents.POTATO_TIMER.get(), timer - 1);
            return;
        }
        if (!(owner instanceof Player player) || player.getMainHandItem() != stack) return;

        float pitch = (float) getCharge(stack) / (float) getMaxCharge(stack) * 0.5F + 0.5F;
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.POTATOS_RANDOM.get(),
                SoundSource.PLAYERS,
                1.0F,
                pitch);
        stack.set(
                ModDataComponents.POTATO_TIMER.get(),
                BASE_DELAY + level.getRandom().nextInt(JITTER));
    }
}
