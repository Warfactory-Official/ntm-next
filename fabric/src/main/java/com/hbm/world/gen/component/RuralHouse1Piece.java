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

import com.hbm.itempool.LootPile;
import com.hbm.itempool.LoreBooks;
import com.hbm.util.RegistryUtil;
import com.hbm.world.gen.component.NtmComponentPiece;
import com.hbm.world.structure.HbmStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class RuralHouse1Piece extends NtmComponentPiece {

    private static final int WIDTH = 14;
    private static final int HEIGHT = 8;
    private static final int DEPTH = 14;

    public RuralHouse1Piece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private RuralHouse1Piece(BlockPos origin, Direction direction) {
        super(
                HbmStructureTypes.NTM_RURAL_HOUSE1_PIECE.get(),
                0,
                StructurePiece.makeBoundingBox(
                        origin.getX(),
                        origin.getY(),
                        origin.getZ(),
                        direction,
                        WIDTH + 1,
                        HEIGHT + 1,
                        DEPTH + 1));
        this.setOrientation(direction);
    }

    public RuralHouse1Piece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_RURAL_HOUSE1_PIECE.get(), context, tag);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    public static final Identifier TEMPLATE_ID = RegistryUtil.id("component/rural_house1");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState concreteExtMachine = hbmState("concrete_ext_machine");
        BlockState oakLog = Blocks.OAK_LOG.defaultBlockState();
        for (int x = 1; x <= 8; x++)
            for (int z = 10; z <= 13; z++)
                this.fillFoundationColumn(level, concreteExtMachine, x, -1, z, chunkBB);
        for (int x = 1; x <= 3; x++)
            for (int z = 4; z <= 9; z++)
                this.fillFoundationColumn(level, concreteExtMachine, x, -1, z, chunkBB);
        for (int x = 4; x <= 13; x++)
            for (int z = 1; z <= 9; z++)
                this.fillFoundationColumn(level, concreteExtMachine, x, -1, z, chunkBB);

        int[][] posts = {
            {2, 0, 3},
            {3, 0, 2},
            {3, -1, 0},
            {5, 0, 0},
            {8, 0, 0},
            {10, -1, 0},
            {14, -1, 1},
            {14, -1, 3},
            {14, 0, 5},
            {14, 0, 6},
            {14, -1, 8},
            {14, -1, 10},
            {9, -1, 14},
            {7, -1, 14},
            {4, 0, 14},
            {5, 0, 14},
            {2, -1, 14},
            {0, -1, 14},
            {0, 0, 13},
            {0, 0, 11},
            {0, -1, 9},
            {0, 0, 6},
            {0, 0, 7},
            {0, 0, 4},
            {0, -1, 3},
            {0, -1, 4}
        };
        for (int[] post : posts)
            this.fillFoundationColumn(level, oakLog, post[0], post[1], post[2], chunkBB);
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockPos pile = this.getWorldPos(3, 2, 12);
        if (!chunkBB.isInside(pile)) return;
        LootPile.placeLore(level, pile, LoreBooks.lab(random), random);
    }
}
