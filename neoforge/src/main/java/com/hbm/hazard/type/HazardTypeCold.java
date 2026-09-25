// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.type;

import com.hbm.config.RadiationConfig;
import com.hbm.data.RadiationData;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.IHazardModifier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeCold implements IHazardType {

    @Override
    public void onUpdate(LivingEntity target, double level, ItemStack stack) {
        boolean reacher = HazardHelper.isHoldingReacher(target);
        if (RadiationData.DISABLE_COLD.get() || reacher) return;
        if (target instanceof Player && ArmorUtil.checkForHazmat(target)) return;

        int baseLevel = (int) level - 1;
        int witherLevel = (int) level - 3;

        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 110, baseLevel));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 110, Math.min(4, baseLevel)));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 110, baseLevel));

        if (level > 4) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 110, witherLevel));
        }
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
        list.add(
                Component.literal("[")
                        .append(Component.translatable("trait.cryogenic"))
                        .append("]")
                        .withStyle(ChatFormatting.AQUA));
    }
}
