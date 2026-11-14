/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.armor;

import com.hbm.extprop.PlayerState;
import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ModLodestoneItem extends ArmorModItem {

    private static final double PULL = 0.75D;
    private static final double LIFT_BELOW = 0.04D;
    private static final double LIFT = 0.2D;

    public final int range;

    public ModLodestoneItem(Properties properties, int range) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
        this.range = range;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.lodestone")
                        .withStyle(ChatFormatting.DARK_GRAY));
        adder.accept(
                Component.translatable("desc.item.armorMod.lodestone.range", range)
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable(
                                "desc.item.armorMod.lodestone.installed",
                                stack.getHoverName(),
                                range)
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean ticksServer() {
        return true;
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level() instanceof ServerLevel level) pull(entity, level);
    }

    private void pull(LivingEntity entity, ServerLevel level) {
        if (entity instanceof Player player && !PlayerState.getOrCreate(player).magnetActive())
            return;

        for (ItemEntity item :
                level.getEntitiesOfClass(
                        ItemEntity.class, entity.getBoundingBox().inflate(range, range, range))) {
            Vec3 towards =
                    new Vec3(
                                    entity.getX() - item.getX(),
                                    entity.getY() - item.getY(),
                                    entity.getZ() - item.getZ())
                            .normalize();
            Vec3 motion = item.getDeltaMovement().add(towards.scale(PULL));
            if (towards.y > 0 && motion.y < LIFT_BELOW) motion = motion.add(0D, LIFT, 0D);
            item.setDeltaMovement(motion);
            level.getChunkSource()
                    .sendToTrackingPlayers(item, new ClientboundSetEntityMotionPacket(item));
        }
    }
}
