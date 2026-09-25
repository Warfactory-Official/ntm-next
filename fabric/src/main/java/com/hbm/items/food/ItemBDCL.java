// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;

public class ItemBDCL extends Item {

    public static final int DRINK_TICKS = 40;
    public static final Consumable CONSUMABLE =
            Consumables.defaultDrink().consumeSeconds(DRINK_TICKS / 20F).build();

    private static final int GULP_EVERY = 5;
    private static final int GULP_UNTIL = 10;
    private static final int HURRY_FROM = 24;
    private static final int HURRY_EVERY = 4;

    public ItemBDCL(Properties props) {
        super(props);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (remaining % GULP_EVERY == 0 && remaining >= GULP_UNTIL) {
            play(level, entity, ModSounds.PLAYER_GULP.get());
        }

        if (remaining == 1) {
            stack.consume(1, entity);
            entity.stopUsingItem();
            play(level, entity, ModSounds.PLAYER_GROAN.get());
            return;
        }

        if (remaining <= HURRY_FROM && remaining % HURRY_EVERY == 0) entity.useItemRemaining--;
    }

    private static void play(Level level, LivingEntity entity, SoundEvent sound) {
        level.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                sound,
                SoundSource.PLAYERS,
                1F,
                1F);
    }
}
