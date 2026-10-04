// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModDataComponents;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemModNightVision extends ItemArmorMod {
    private static final int DURATION = 15 * SharedConstants.TICKS_PER_SECOND;

    public ItemModNightVision(Properties properties) {
        super(properties, ArmorModHandler.HELMET_ONLY, true, false, false, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.nightVision")
                        .withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable(
                                "desc.item.armorMod.nightVision.installed", stack.getHoverName())
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean ticksServer() {
        return true;
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide() || !(entity instanceof Player player)) return;
        if (!(armor.getItem() instanceof ModArmorItem suit) || !suit.isPowered()) return;
        if (!ArmorSuitEffects.hasFullSet(player, suit.suit())) return;

        if (HbmPlayerProps.getData(player).enableHUD) {
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION, 0));
            if (!armor.has(ModDataComponents.ARMOR_MOD_NV_ACTIVE.get()))
                armor.set(ModDataComponents.ARMOR_MOD_NV_ACTIVE.get(), Unit.INSTANCE);
            if (entity.getRandom().nextInt(200) == 0)
                ArmorUtil.drainSupplyForWear(entity, armor, 1);
        } else if (armor.has(ModDataComponents.ARMOR_MOD_NV_ACTIVE.get())) {
            entity.removeEffect(MobEffects.NIGHT_VISION);
            armor.remove(ModDataComponents.ARMOR_MOD_NV_ACTIVE.get());
        }
    }
}
