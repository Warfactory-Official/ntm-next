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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.StructureAssemblyBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.ICFStructBlock;
import com.hbm.blocks.multiblock.AssemblyPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class ICFAssemblyBlockEntity extends StructureAssemblyBlockEntity {

    public ICFAssemblyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ICF_STRUCT.get(), pos, state);
    }

    public static void visitCells(AssemblyPattern.CellConsumer visitor) {
        var pattern = Pattern.VALUE;
        for (int cell = 0; cell < pattern.size(); cell++) {
            long offset = pattern.getOffset(cell);
            visitor.accept(
                    pattern.getBlock(cell),
                    BlockPos.getZ(offset),
                    BlockPos.getY(offset),
                    -BlockPos.getX(offset));
        }
    }

    public BlockPos cellPosition(Direction facing, int width, int height, int length) {
        var across = facing.getClockWise();
        return worldPosition.offset(
                width * facing.getStepX() + length * across.getStepX(),
                height,
                width * facing.getStepZ() + length * across.getStepZ());
    }

    private static final class Pattern {
        private static final AssemblyPattern VALUE = build();

        private static AssemblyPattern build() {
            var builder = new AssemblyPattern.Builder(1, true);
            for (int length = -8; length <= 8; length++) {
                for (int width = -2; width <= 2; width++) {
                    for (int height = 0; height <= 5; height++) {
                        Block material;
                        if (height == 0) {
                            if (Math.abs(width) > 1 || width == 0 && length == 0) continue;
                            material = ModBlocks.ICF_COMPONENT.get();
                        } else if (height == 3 && width == 0) {
                            material = ModBlocks.ICF_COMPONENT_VESSEL_WELDED.get();
                        } else {
                            if ((height == 1 || height == 5) && Math.abs(width) > 1) continue;
                            material =
                                    Math.abs(length) <= 2
                                            ? ModBlocks.ICF_COMPONENT_VESSEL_WELDED.get()
                                            : ModBlocks.ICF_COMPONENT_STRUCTURE_BOLTED.get();
                        }
                        builder.add(material, -length, height, width);
                    }
                }
            }
            return builder.build();
        }
    }

    @Override
    public AssemblyPattern getPattern() {
        return Pattern.VALUE;
    }

    @Override
    public Direction getAssemblyFacing() {
        return getBlockState().getValue(ICFStructBlock.FACING);
    }

    @Override
    public AABB getPreviewBounds() {
        return new AABB(-9, -1, -9, 10, 7, 10);
    }

    @Override
    protected void assemble(int variant) {
        ModBlocks.MACHINE_ICF.get().placeAssembled(level, worldPosition, getAssemblyFacing());
    }
}
