// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.NuclearTech;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemSyringeMku extends Item {

    public ItemSyringeMku(Properties props) {
        super(props);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(target.level() instanceof ServerLevel level)) return;
        HbmLivingProps.getData(target).contagion = EntityEffectHandler.CONTAGION_DURATION;
        level.playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                ModSounds.ITEM_SYRINGE.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        stack.shrink(1);
        if (Services.CONFIG.runtime().extendedLogging()) {
            NuclearTech.LOGGER.info(
                    "[MKU] {} used an MKU syringe!", attacker.getName().getString());
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag tooltipFlag) {
        adder.accept(Component.literal("?").withStyle(ChatFormatting.RED));
    }
}
