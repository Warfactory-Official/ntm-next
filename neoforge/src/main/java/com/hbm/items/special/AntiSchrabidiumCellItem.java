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

import com.hbm.data.ExplosionData;
import com.hbm.data.ItemData;
import com.hbm.entity.effect.CloudFleijaEntity;
import com.hbm.entity.logic.NukeExplosionMK3Entity;
import com.hbm.interfaces.DroppedItemUpdateHandler;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidCellItem;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class AntiSchrabidiumCellItem extends FluidCellItem implements DroppedItemUpdateHandler {

    public AntiSchrabidiumCellItem(Properties properties) {
        super(properties, () -> NTMFluids.ASCHRAB, ModItems.CELL_EMPTY);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.onGround()) return false;

        if (ItemData.DROP_ANTIMATTER_CELL.get()) {
            Level level = entity.level();
            NukeExplosionMK3Entity blast =
                    NukeExplosionMK3Entity.statFacFleija(
                            level,
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            ExplosionData.A_SCHRAB_RADIUS.get());
            if (!blast.isRemoved()) {
                level.playSound(
                        null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.GENERIC_EXPLODE.value(),
                        SoundSource.BLOCKS,
                        100.0F,
                        level.getRandom().nextFloat() * 0.1F + 0.9F);
                level.addFreshEntity(blast);
                level.addFreshEntity(
                        CloudFleijaEntity.statFac(
                                level,
                                ExplosionData.A_SCHRAB_RADIUS.get(),
                                entity.getX(),
                                entity.getY(),
                                entity.getZ()));
            }
        }
        entity.discard();
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.shared.warningExposureToMatter"));
        adder.accept(Component.translatable("desc.item.antiSchrabidiumCell.createAFLkvangr"));
        adder.accept(
                Component.literal("[")
                        .append(Component.translatable("trait.drop"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
