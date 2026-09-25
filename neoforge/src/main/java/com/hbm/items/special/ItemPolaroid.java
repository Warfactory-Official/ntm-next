// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.main.Polaroid;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class ItemPolaroid extends Item {

    private static final float THRESHOLD = 10F;
    private static final int DURATION = 10;
    private static final int AMPLIFIER = 2;

    public ItemPolaroid(Properties props) {
        super(props);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (owner instanceof Player player && player.getHealth() < THRESHOLD) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, DURATION, AMPLIFIER));
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.polaroid"));
        adder.accept(Component.empty());
        for (String line : Polaroid.loreLines("desc.item.polaroid."))
            adder.accept(Component.literal(line));
    }
}
