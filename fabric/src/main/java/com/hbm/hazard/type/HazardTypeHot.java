// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.type;

import com.hbm.config.RadiationConfig;
import com.hbm.data.RadiationData;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.IHazardModifier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeHot implements IHazardType {

    @Override
    public void onUpdate(LivingEntity target, double level, ItemStack stack) {
        boolean wetOrReacher = HazardHelper.isHoldingReacher(target) || target.isInWaterOrRain();
        if (RadiationData.DISABLE_HOT.get() || wetOrReacher || level <= 0) return;
        target.setRemainingFireTicks((int) Math.ceil(level) * RadiationConfig.hazardRate);
    }

    @Override
    public void updateEntity(ItemEntity item, double level) {}

    @Override
    public void addHazardInformation(
            Player player,
            List<Component> list,
            double level,
            ItemStack stack,
            List<IHazardModifier> modifiers) {
        level = IHazardModifier.evalAllModifiers(stack, player, level, modifiers);
        if (level > 0) {
            list.add(
                    Component.literal("[")
                            .append(Component.translatable("trait.hot"))
                            .append("]")
                            .withStyle(ChatFormatting.GOLD));
        }
    }
}
