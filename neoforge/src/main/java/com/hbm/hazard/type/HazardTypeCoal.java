// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.type;

import com.hbm.config.RadiationConfig;
import com.hbm.data.RadiationData;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.HazardClass;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.util.ArmorRegistry;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeCoal implements IHazardType {

    @Override
    public void onUpdate(LivingEntity target, double level, ItemStack stack) {
        if (RadiationData.DISABLE_COAL.get()) return;
        if (!ArmorRegistry.hasProtection(target, EquipmentSlot.HEAD, HazardClass.PARTICLE_COARSE)) {
            HbmLivingProps.incrementBlackLung(
                    target,
                    (int) Math.min(level * stack.getCount(), 10) * RadiationConfig.hazardRate);
        } else if (target.getRandom().nextInt(Math.max(65 - stack.getCount(), 1)) == 0) {
            ArmorUtil.damageGasMaskFilter(target, (int) level * RadiationConfig.hazardRate);
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
                        .append(Component.translatable("trait.coal"))
                        .append("]")
                        .withStyle(ChatFormatting.DARK_GRAY));
    }
}
