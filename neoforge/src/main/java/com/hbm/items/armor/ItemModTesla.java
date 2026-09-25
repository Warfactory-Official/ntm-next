// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.tileentity.machine.BlockEntityTesla;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemModTesla extends ItemArmorMod {

    private static final double EYE_OFFSET = 1.25D;
    private static final double RANGE = 5D;

    public ItemModTesla(Properties properties) {
        super(properties, ArmorModHandler.PLATE_ONLY, false, true, false, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.tesla")
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {

        tooltip.add(
                Component.translatable("desc.item.armorMod.tesla.installed", stack.getHoverName())
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!(entity.level() instanceof ServerLevel level) || !(entity instanceof Player player))
            return;
        if (!(armor.getItem() instanceof ModArmorItem suit) || !suit.isPowered()) return;
        if (!ArmorSuitEffects.hasFullSet(player, suit.suit())) return;

        if (!BlockEntityTesla.zap(
                                level,
                                entity.getX(),
                                entity.getY() + EYE_OFFSET,
                                entity.getZ(),
                                RANGE,
                                entity)
                        .isEmpty()
                && entity.getRandom().nextInt(5) == 0) {
            ArmorUtil.drainSupplyForWear(entity, armor, 1);
        }
    }
}
