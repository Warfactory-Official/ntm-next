/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.blocks.machine.pile;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.ToolType;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class GraphiteDrilledBlock extends DrilledGraphiteBlock {

    public GraphiteDrilledBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {

        if (stack.isEmpty()) return InteractionResult.PASS;
        if (!isChannelFace(state, hit.getDirection())) return InteractionResult.PASS;

        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_URANIUM.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_FUEL.get().defaultBlockState())))
            return InteractionResult.SUCCESS;
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_PU239.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_FUEL.get().defaultBlockState())
                        .setValue(PU239, true))) return InteractionResult.SUCCESS;
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_PLUTONIUM.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get().defaultBlockState())))
            return InteractionResult.SUCCESS;
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_SOURCE.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_SOURCE.get().defaultBlockState())))
            return InteractionResult.SUCCESS;
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_BORON.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_ROD.get().defaultBlockState())))
            return InteractionResult.SUCCESS;
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_LITHIUM.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_LITHIUM.get().defaultBlockState())))
            return InteractionResult.SUCCESS;
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.CELL_TRITIUM.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_TRITIUM.get().defaultBlockState())))
            return InteractionResult.SUCCESS;
        if (checkInteraction(
                level,
                pos,
                stack,
                ModItems.PILE_ROD_DETECTOR.get(),
                copyShape(state, ModBlocks.BLOCK_GRAPHITE_DETECTOR.get().defaultBlockState())))
            return InteractionResult.SUCCESS;

        if (!state.getValue(SHROUDED)) {
            if (checkInteraction(
                    level, pos, stack, aluminiumShell(), state.setValue(SHROUDED, true)))
                return InteractionResult.SUCCESS;
            if (checkInteraction(
                    level,
                    pos,
                    stack,
                    ModItems.ingot(Materials.GRAPHITE),
                    ModBlocks.BLOCK_GRAPHITE.get().defaultBlockState()))
                return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private boolean checkInteraction(
            Level level, BlockPos pos, ItemStack stack, @Nullable Item item, BlockState result) {

        if (item == null || stack.getItem() != item) return false;

        stack.shrink(1);
        level.setBlock(pos, result, 3);

        level.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY() + 1.5,
                pos.getZ() + 0.5,
                ModSounds.UPGRADE_PLUG.get(),
                SoundSource.BLOCKS,
                1.0F,
                1.0F);

        return true;
    }

    @Override
    public boolean onToolUse(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {

        if (tool != ToolType.SCREWDRIVER) return false;

        BlockState state = level.getBlockState(pos);

        if (!level.isClientSide() && isChannelFace(state, side) && state.getValue(SHROUDED)) {
            level.setBlock(pos, state.setValue(SHROUDED, false), 3);
            level.playSound(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 1.5,
                    pos.getZ() + 0.5,
                    ModSounds.UPGRADE_PLUG.get(),
                    SoundSource.BLOCKS,
                    1.0F,
                    0.85F);

            ejectItem(level, pos, side, new ItemStack(aluminiumShell()));
        }

        return true;
    }
}
