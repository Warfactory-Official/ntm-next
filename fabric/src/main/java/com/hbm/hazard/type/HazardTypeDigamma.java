// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.type;

import com.hbm.config.RadiationConfig;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.util.ContaminationUtil;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeDigamma implements IHazardType {

    @Override
    public void onUpdate(LivingEntity target, double level, ItemStack stack) {
        ContaminationUtil.applyDigammaData(target, (level / 20D) * RadiationConfig.hazardRate);
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
        double displayLevel = Math.floor(level * 10000D) / 10D;
        list.add(
                Component.literal("[")
                        .append(Component.translatable("trait.digamma"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
        list.add(Component.literal(displayLevel + "mDRX/s").withStyle(ChatFormatting.DARK_RED));
        if (stack.getCount() > 1) {
            double stackLevel = Math.floor(level * 10000D * stack.getCount()) / 10D;
            list.add(
                    Component.translatable(
                                    "desc.misc.hazardTypeDigamma.stack", stackLevel + "mDRX/s")
                            .withStyle(ChatFormatting.DARK_RED));
        }
    }
}
