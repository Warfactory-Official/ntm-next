// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.type;

import com.hbm.config.RadiationConfig;
import com.hbm.data.RadiationData;
import com.hbm.hazard.modifier.IHazardModifier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HazardTypeHydroactive implements IHazardType {

    @Override
    public void onUpdate(LivingEntity target, double level, ItemStack stack) {
        if (RadiationData.DISABLE_HYDRO.get()) return;
        if (target.isInWaterOrRain() && stack.getCount() > 0) {
            stack.setCount(0);

            target.level()
                    .explode(
                            null,
                            target.getX(),
                            target.getEyeY(),
                            target.getZ(),
                            (float) level,
                            false,
                            Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override
    public void updateEntity(ItemEntity item, double level) {
        if (RadiationData.DISABLE_HYDRO.get()) return;
        if (item.isInWaterOrRain()) {
            item.discard();

            item.level()
                    .explode(
                            null,
                            item.getX(),
                            item.getY() + item.getBbHeight() * 0.5,
                            item.getZ(),
                            (float) level,
                            false,
                            Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override
    public void addHazardInformation(
            Player player,
            List<Component> list,
            double level,
            ItemStack stack,
            List<IHazardModifier> modifiers) {
        list.add(
                Component.literal("[")
                        .append(Component.translatable("trait.hydro"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
