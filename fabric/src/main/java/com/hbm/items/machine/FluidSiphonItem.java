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

package com.hbm.items.machine;

import com.hbm.api.fluidmk2.FluidTankHandler;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.FluidContainerRows;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Unsiphonable;
import com.hbm.items.tool.PipetteItem;
import com.hbm.platform.Caps;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class FluidSiphonItem extends Item {
    private static final int PIPETTE_REMAINDER = 1000;

    public FluidSiphonItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos target = MultiblockSurface.coreOfAny(level, ctx.getClickedPos());
        if (target == null) target = ctx.getClickedPos();

        BlockEntity be = level.getBlockEntity(target);
        Player player = ctx.getPlayer();
        if (!(be instanceof FluidTankHandler endpoint) || player == null)
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        for (FluidTankNTM tank : endpoint.getReceivingTanks()) {
            Fluid type = tank.getTankType();
            if (tank.getFill() <= 0 || type == null) continue;
            if (NTMFluidProperties.hasTrait(type, FT_Unsiphonable.class)) continue;
            if (drain(player, tank, tank.getFluid())) {
                be.setChanged();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private boolean drain(Player player, FluidTankNTM tank, Fluid type) {
        Inventory inventory = player.getInventory();
        PipetteItem pipette = null;
        ItemStack pipetteStack = ItemStack.EMPTY;
        boolean drained = false;

        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;

            if (pipette == null
                    && stack.getItem() instanceof PipetteItem held
                    && !held.fizzlesOn(type)
                    && held.variant() != PipetteItem.Variant.LABORATORY) {
                pipette = held;
                pipetteStack = stack;
            }

            ItemStack filled = fillOne(stack, tank, type);
            while (filled != null) {
                drained = true;
                stack.shrink(1);
                player.getInventory().placeItemBackInInventory(filled, Prediction.SERVER_ONLY);
                if (stack.isEmpty()) break;
                filled = fillOne(stack, tank, type);
            }
        }

        if (pipette != null && tank.getFill() < PIPETTE_REMAINDER) {
            int taken = pipette.fill(pipetteStack, type, tank.getFill(), tank.getPressure());
            if (taken > 0) {
                tank.setFill(tank.getFill() - taken);
                drained = true;
            }
        }
        return drained;
    }

    private @Nullable ItemStack fillOne(ItemStack stack, FluidTankNTM tank, Fluid type) {
        if (stack.getItem() instanceof PipetteItem) return null;
        if (FluidContainerRows.answers(stack)) {
            int amount = FluidContainerRows.fillAmount(stack, type);
            if (amount <= 0 || amount > tank.getFill()) return null;
            tank.setFill(tank.getFill() - amount);
            return FluidContainerRows.filled(stack, type);
        }
        SimpleContainer scratch = new SimpleContainer(stack.copyWithCount(1));
        Storage<FluidVariant> store = Caps.findFluidStore(scratch, 0);
        if (store == null) return null;

        long room = Caps.insertable(store, type);
        if (room <= 0 || room > tank.getFill()) return null;
        long given = Caps.moveWholeMb(store, FluidVariant.of(type), room, true);
        if (given <= 0) return null;

        tank.setFill(tank.getFill() - given);
        return scratch.getItem(0);
    }
}
