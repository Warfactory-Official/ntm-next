/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.world.gen.component;

import com.hbm.util.RegistryUtil;
import com.hbm.world.structure.HbmStructureTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class NTMHouse2Piece extends NtmComponentPiece {

    private static final int WIDTH = 15;
    private static final int HEIGHT = 5;
    private static final int DEPTH = 9;

    public NTMHouse2Piece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private NTMHouse2Piece(BlockPos origin, Direction direction) {
        super(
                HbmStructureTypes.NTM_HOUSE2_PIECE.get(),
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

    public NTMHouse2Piece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_HOUSE2_PIECE.get(), context, tag);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    public static final Identifier TEMPLATE_ID = RegistryUtil.id("component/house2");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 15; z++)
                this.fillFoundationColumn(level, sandstone, x, -1, z, chunkBB);
        }
        for (int x = 9; x <= 15; x++) {
            for (int z = 0; z <= 9; z++)
                this.fillFoundationColumn(level, sandstone, x, -1, z, chunkBB);
        }
    }
}
