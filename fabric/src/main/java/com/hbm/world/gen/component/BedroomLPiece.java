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
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class BedroomLPiece extends NtmBranchingPiece {

    private static final int WIDTH = 10, HEIGHT = 6, DEPTH = 11;

    private boolean path;

    private BedroomLPiece(BoundingBox box, Direction direction) {
        super(HbmStructureTypes.NTM_BUNKER_BEDROOM_L.get(), 0, box);
        this.setOrientation(direction);
    }

    public BedroomLPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_BUNKER_BEDROOM_L.get(), context, tag);
        this.path = tag.getBooleanOr("Path", false);
    }

    public static StructurePiece findValidPlacement(
            StructurePieceAccessor accessor,
            RandomSource random,
            int x,
            int y,
            int z,
            Direction direction) {
        BoundingBox box = anchorBoundingBox(x, y, z, direction, -8, -1, 0, WIDTH, HEIGHT, DEPTH);
        if (box.minY() <= 10 || accessor.findCollisionPiece(box) != null) return null;
        return new BedroomLPiece(box, direction);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("Path", path);
    }

    @Override
    public void buildComponent(
            NtmBranchingStart start, StructurePieceAccessor accessor, RandomSource random) {
        this.path = this.generateChildWest(start, accessor, random, 9, 1) != null;
    }

    public static final Identifier TEMPLATE_ID = RegistryUtil.id("component/bedroom_l");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        if (this.path) this.generateAirBox(level, chunkBB, 0, 1, 8, 0, 2, 9);
    }
}
