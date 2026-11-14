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

package com.hbm.items.special;

import com.hbm.data.ItemData;
import com.hbm.entity.effect.QuasarEntity;
import com.hbm.interfaces.DroppedItemUpdateHandler;
import com.hbm.util.ContaminationUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class DigammaItem extends Item implements DroppedItemUpdateHandler {

    private final int digamma;

    public DigammaItem(Properties properties, int digamma) {
        super(properties);
        this.digamma = digamma;
    }

    private static final float QUASAR_SIZE = 5F;

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.onGround() || !(entity.level() instanceof ServerLevel level)) return false;

        if (ItemData.DROP_SINGULARITY.get()) {
            QuasarEntity quasar = new QuasarEntity(level, QUASAR_SIZE);
            quasar.setPos(entity.getX(), entity.getY(), entity.getZ());
            level.addFreshEntity(quasar);
        }
        entity.discard();
        return true;
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (entity instanceof LivingEntity living) {
            ContaminationUtil.applyDigammaData(living, 1.0 / digamma);
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable("trait.hlParticle", "1.67*10³⁴ a")
                        .withStyle(ChatFormatting.GOLD));
        adder.accept(
                Component.translatable("trait.hlPlayer", (digamma / 20D) + "s")
                        .withStyle(ChatFormatting.RED));
        adder.accept(Component.empty());

        double mdrxPerSecond = ((int) ((1000D / digamma) * 200D)) / 10D;
        adder.accept(
                Component.literal("[")
                        .append(Component.translatable("trait.digamma"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
        adder.accept(
                Component.literal(mdrxPerSecond + "mDRX/s").withStyle(ChatFormatting.DARK_RED));

        adder.accept(
                Component.literal("[")
                        .append(Component.translatable("trait.drop"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
