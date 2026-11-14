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

package com.hbm.world.gen.component;

import com.hbm.itempool.LoreBooks;
import com.hbm.util.RegistryUtil;
import com.hbm.world.structure.HbmStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class NTMLab2Piece extends NtmComponentPiece {

    private static final int WIDTH = 12;
    private static final int HEIGHT = 11;
    private static final int DEPTH = 8;

    public NTMLab2Piece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private NTMLab2Piece(BlockPos origin, Direction direction) {
        super(
                HbmStructureTypes.NTM_LAB2_PIECE.get(),
                0,
                StructurePiece.makeBoundingBox(
                        origin.getX(),
                        origin.getY() - 7,
                        origin.getZ(),
                        direction,
                        WIDTH + 1,
                        HEIGHT + 1,
                        DEPTH + 1));
        this.setOrientation(direction);
    }

    public NTMLab2Piece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_LAB2_PIECE.get(), context, tag);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    public static final Identifier TEMPLATE_ID = RegistryUtil.id("component/lab2");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState stoneBricks = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 6; z++)
                this.fillFoundationColumn(level, stoneBricks, x, 6, z, chunkBB);
        }
        for (int x = 0; x <= 6; x++) {
            for (int z = 7; z <= 8; z++)
                this.fillFoundationColumn(level, stoneBricks, x, 6, z, chunkBB);
        }
        if (this.getBlock(level, 9, 7, 7, chunkBB).canBeReplaced()) {
            this.fillFoundationColumn(level, stoneBricks, 9, 7, 7, chunkBB);
            this.fillFoundationColumn(level, stoneBricks, 10, 7, 7, chunkBB);
            BlockState stair =
                    Blocks.STONE_BRICK_STAIRS
                            .defaultBlockState()
                            .setValue(StairBlock.FACING, Direction.NORTH)
                            .setValue(StairBlock.HALF, Half.BOTTOM);
            this.generateBox(level, chunkBB, 9, 7, 7, 10, 7, 7, stair, stair, false);
        }
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        if (!chunkBB.isInside(this.getWorldPos(10, 1, 3))) return;
        if (random.nextInt(2) == 0) {
            this.generateLoreBook(level, chunkBB, 10, 1, 3, 1, LoreBooks.office(random));
        }
    }
}
