// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ItemGasFilter extends Item {

    public ItemGasFilter(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack filter = player.getItemInHand(hand);
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return InteractionResult.PASS;

        boolean socketed = !(helmet.getItem() instanceof IGasMask);
        ItemStack target =
                socketed ? ArmorModHandler.pryMod(helmet, ArmorModHandler.HELMET_ONLY) : helmet;
        if (!(target.getItem() instanceof IGasMask)) return InteractionResult.PASS;

        ItemStack current = ArmorUtil.getGasMaskFilter(target);
        ArmorUtil.installGasMaskFilter(target, filter.copyWithCount(1));
        if (socketed) ArmorModHandler.applyMod(helmet, target);
        if (current.isEmpty()) {
            filter.shrink(1);
        } else {
            player.setItemInHand(hand, current);
        }
        level.playSound(
                null,
                player.blockPosition(),
                ModSounds.GASMASK_SCREW.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        return InteractionResult.SUCCESS;
    }
}
