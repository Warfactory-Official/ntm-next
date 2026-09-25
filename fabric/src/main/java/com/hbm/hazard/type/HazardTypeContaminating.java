// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard.type;

import com.hbm.config.RadiationConfig;
import com.hbm.data.RadiationData;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.hazard.modifier.IHazardModifier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class HazardTypeContaminating implements IHazardType {

    private static final int MAX_RADIUS = 500;

    private static int computeRadius(double level) {
        return (int) Math.min(Math.sqrt(level) + 0.5D, MAX_RADIUS);
    }

    @Override
    public void onUpdate(LivingEntity target, double level, ItemStack stack) {}

    @Override
    public void updateEntity(ItemEntity item, double level) {
        if (!RadiationData.ENABLE_CONTAMINATION_ON_GROUND.get()) return;
        Level world = item.level();
        if (!(world instanceof ServerLevel server)) return;
        if (!item.onGround()) return;

        int radius = computeRadius(level);
        if (radius > 1) {
            BlockPos pos = item.blockPosition();
            RadiationSystemNT.incrementRad(server, pos, level, level * 1024D);
        }
        item.discard();
    }

    @Override
    public void addHazardInformation(
            Player player,
            List<Component> list,
            double level,
            ItemStack stack,
            List<IHazardModifier> modifiers) {
        if (!RadiationData.ENABLE_CONTAMINATION_ON_GROUND.get()) return;
        int radius = computeRadius(level);
        if (radius > 1) {
            list.add(
                    Component.literal("[")
                            .append(Component.translatable("trait.contaminating"))
                            .append("]")
                            .withStyle(ChatFormatting.DARK_GREEN));
            list.add(
                    Component.literal(" ")
                            .append(Component.translatable("trait.contaminating.radius", radius))
                            .withStyle(ChatFormatting.GREEN));
        }
    }
}
