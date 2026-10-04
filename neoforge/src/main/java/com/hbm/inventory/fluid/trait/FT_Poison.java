// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory.fluid.trait;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class FT_Poison extends FluidTrait {

    private boolean withering;
    private int level;

    public FT_Poison() {}

    public FT_Poison(boolean withering, int level) {
        this.withering = withering;
        this.level = level;
    }

    public boolean isWithering() {
        return withering;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public void addInfoHidden(List<Component> info) {
        info.add(
                Component.empty()
                        .append(Component.literal("[").withStyle(ChatFormatting.GREEN))
                        .append(
                                Component.translatable("trait.toxicfumes")
                                        .withStyle(ChatFormatting.GREEN))
                        .append(Component.literal("]").withStyle(ChatFormatting.GREEN)));
    }

    public void affect(LivingEntity living, double intensity) {
        living.addEffect(
                new MobEffectInstance(
                        withering ? MobEffects.WITHER : MobEffects.POISON,
                        (int) (5 * SharedConstants.TICKS_PER_SECOND * intensity)));
    }
}
