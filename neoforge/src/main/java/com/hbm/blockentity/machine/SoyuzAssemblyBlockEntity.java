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
import com.hbm.blocks.machine.SoyuzLauncherBlock;
import com.hbm.blocks.multiblock.AssemblyPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class SoyuzAssemblyBlockEntity extends StructureAssemblyBlockEntity {
    private static final Box[] PAD = {
        new Box(-6, 6, 3, 4, -6, 6),
        new Box(-1, 1, 3, 4, -8, -7),
        new Box(-2, 2, 3, 4, 7, 9),
        new Box(-2, 2, 51, 51, 5, 9),
        new Box(-1, 1, 38, 38, -8, -6),
    };
    private static final Box[] LEGS = {
        new Box(3, 6, 0, 2, 3, 6),
        new Box(-6, -3, 0, 2, 3, 6),
        new Box(-6, -3, 0, 2, -6, -3),
        new Box(3, 6, 0, 2, -6, -3),
        new Box(-1, 1, 0, 2, -8, -6),
        new Box(-2, 2, 0, 2, 5, 9),
    };
    private static final Box[] SCAFFOLD = {
        new Box(-1, 1, 5, 50, 6, 8), new Box(0, 0, 5, 37, -7, -7),
    };
    private static final Box[] CLEARED = {
        new Box(-2, 2, 51, 51, 5, 9),
        new Box(-1, 1, 38, 38, -8, -6),
        new Box(-2, 2, 0, 2, 5, 9),
        new Box(-1, 1, 5, 50, 6, 8),
        new Box(0, 0, 5, 37, -7, -7),
    };

    private static final class Pattern {
        private static final AssemblyPattern VALUE = build();

        private static AssemblyPattern build() {
            var builder = new AssemblyPattern.Builder(1, false);
            emit(
                    (block, x, y, z) -> builder.add(block, x, y, z),
                    PAD,
                    ModBlocks.STRUCT_LAUNCHER.get());
            emit(
                    (block, x, y, z) -> builder.add(block, ModBlocks.CONCRETE.get(), 1, x, y, z),
                    LEGS,
                    ModBlocks.CONCRETE_SMOOTH.get());
            emit(
                    (block, x, y, z) -> builder.add(block, x, y, z),
                    SCAFFOLD,
                    ModBlocks.STRUCT_SCAFFOLD.get());
            return builder.build();
        }
    }

    @Override
    public AssemblyPattern getPattern() {
        return Pattern.VALUE;
    }

    @Override
    public boolean usesSouthPreviewNormal() {
        return true;
    }

    @Override
    public AABB getPreviewBounds() {
        return new AABB(-6, 0, -8, 7, 52, 10);
    }

    @Override
    public AABB getVisualBounds() {
        return getPreviewBounds().inflate(1);
    }

    public SoyuzAssemblyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOYUZ_STRUCT.get(), pos, state);
    }

    public static void forEachRequirement(AssemblyPattern.CellConsumer out) {
        emit(out, PAD, ModBlocks.STRUCT_LAUNCHER.get());
        emit(out, LEGS, ModBlocks.CONCRETE_SMOOTH.get());
        emit(out, SCAFFOLD, ModBlocks.STRUCT_SCAFFOLD.get());
    }

    private static void emit(AssemblyPattern.CellConsumer out, Box[] boxes, Block block) {
        for (Box box : boxes) box.forEach((dx, dy, dz) -> out.accept(block, dx, dy, dz));
    }

    @Override
    protected void assemble(int variant) {

        for (Box box : CLEARED) {
            box.forEach((dx, dy, dz) -> level.removeBlock(worldPosition.offset(dx, dy, dz), false));
        }

        level.removeBlock(worldPosition, false);
        ModBlocks.SOYUZ_LAUNCHER
                .get()
                .placeAssembled(
                        level,
                        worldPosition.above(SoyuzLauncherBlock.HEIGHT),
                        SoyuzLauncherBlock.FACING);
    }

    private interface CellVisitor {
        void accept(int dx, int dy, int dz);
    }

    private record Box(int x0, int x1, int y0, int y1, int z0, int z1) {

        void forEach(CellVisitor out) {
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) out.accept(x, y, z);
                }
            }
        }
    }
}
