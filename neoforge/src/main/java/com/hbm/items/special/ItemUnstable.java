// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.handler.UnstableFuses;
import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import com.hbm.util.GameTime;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public final class ItemUnstable extends Item implements IItemEntityUpdate {

    public static final int RADIUS = 350;
    public static final int TIMER = 200;

    public ItemUnstable(Properties properties) {
        super(properties);
    }

    private static int getTimer(ItemStack stack, long gameTime) {
        Long ends = stack.get(ModDataComponents.UNSTABLE_AT.get());
        if (ends == null) return 0;
        return (int) Math.max(0L, Math.min(TIMER, TIMER - (ends - gameTime)));
    }

    public static boolean hasSpentFuse(ItemStack stack, long gameTime) {
        if (!(stack.getItem() instanceof ItemUnstable)) return false;
        Long ends = stack.get(ModDataComponents.UNSTABLE_AT.get());
        return ends != null && gameTime >= ends;
    }

    public static void detonate(ServerLevel level, double x, double y, double z) {
        level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, RADIUS, x, y, z));
        level.playSound(
                null,
                BlockPos.containing(x, y, z),
                ModSounds.OLD_EXPLOSION.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
    }

    private static void arm(ItemStack stack, ServerLevel level) {
        if (stack.isEmpty()) return;
        long now = level.getGameTime();
        Long ends = stack.get(ModDataComponents.UNSTABLE_AT.get());
        if (ends == null) {

            ends = now + TIMER - 1;
            stack.set(ModDataComponents.UNSTABLE_AT.get(), ends);
        }
        UnstableFuses.arm(level, ends);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        arm(stack, level);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (entity.level() instanceof ServerLevel level) arm(entity.getItem(), level);
        return false;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable(
                        "desc.item.unstable.decay",
                        (getTimer(stack, GameTime.ticks()) * 100 / TIMER) + "%"));
    }
}
