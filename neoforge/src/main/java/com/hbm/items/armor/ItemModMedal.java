// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.util.ContaminationUtil;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemModMedal extends ItemArmorMod {

    private static final float ACTIVATION_DECAY = (float) Math.pow(0.5, 0.5F / 6000);

    public ItemModMedal(Properties properties) {
        super(properties, ArmorModHandler.PLATE_ONLY, true, true, false, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.medal").withStyle(ChatFormatting.GOLD));
    }

    @Override
    protected boolean hasTooltipSpacer() {
        return false;
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable("desc.item.armorMod.medal.installed", stack.getHoverName())
                        .withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide()) return;
        HbmLivingProps.setRadiation(
                entity, Math.max(HbmLivingProps.getRadiation(entity) - 0.5D, 0D));

        if (entity instanceof Player player)
            ContaminationUtil.neutronActivateInventory(player, 0F, ACTIVATION_DECAY);
    }
}
