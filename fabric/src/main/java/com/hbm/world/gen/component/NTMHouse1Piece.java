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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class NTMHouse1Piece extends NtmComponentPiece {

    public static final Identifier TEMPLATE_ID = RegistryUtil.id("component/house1");

    private static final int WIDTH = 9;
    private static final int HEIGHT = 4;
    private static final int DEPTH = 6;

    public NTMHouse1Piece(BlockPos origin, RandomSource random) {
        this(origin, getRandomHorizontalDirection(random));
    }

    private NTMHouse1Piece(BlockPos origin, Direction direction) {
        super(
                HbmStructureTypes.NTM_HOUSE1_PIECE.get(),
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

    public NTMHouse1Piece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_HOUSE1_PIECE.get(), context, tag);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {}

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildFoundation(WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        for (int x = 0; x <= WIDTH; x++) {
            for (int z = 0; z <= DEPTH; z++) {
                this.fillFoundationColumn(
                        level, Blocks.SANDSTONE.defaultBlockState(), x, -1, z, chunkBB);
            }
        }
    }
}
