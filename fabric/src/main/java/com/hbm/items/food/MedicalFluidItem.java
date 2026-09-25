// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.items.special.ItemCustomLore;
import com.hbm.potion.HbmPotion;
import com.hbm.sound.ModSounds;
import com.hbm.util.InventoryUtil;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MedicalFluidItem extends ItemCustomLore {

    private final Supplier<? extends Item> emptyIv;
    private final int ticks;

    public MedicalFluidItem(Properties properties, Supplier<? extends Item> emptyIv, int ticks) {
        super(properties);
        this.emptyIv = emptyIv;
        this.ticks = ticks;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        ItemStack held =
                InventoryUtil.exchangeHeld(
                        player, player.getItemInHand(hand), new ItemStack(emptyIv.get()));
        server.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.ITEM_RADAWAY.get(),
                SoundSource.PLAYERS,
                1F,
                1F);
        MobEffectInstance existing = player.getEffect(HbmPotion.radaway());
        int duration = existing == null ? ticks : existing.getDuration() + ticks;
        player.addEffect(new MobEffectInstance(HbmPotion.radaway(), duration, 0));
        return InteractionResult.SUCCESS.heldItemTransformedTo(held);
    }
}
