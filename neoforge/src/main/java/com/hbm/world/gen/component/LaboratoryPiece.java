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
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class LaboratoryPiece extends NtmBranchingPiece {

    private static final int WIDTH = 9, HEIGHT = 6, DEPTH = 12;

    private boolean pathWest;
    private boolean pathNormal;

    private LaboratoryPiece(BoundingBox box, Direction direction) {
        super(HbmStructureTypes.NTM_BUNKER_LABORATORY.get(), 0, box);
        this.setOrientation(direction);
    }

    public LaboratoryPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_BUNKER_LABORATORY.get(), context, tag);
        this.pathWest = tag.getBooleanOr("PathWest", false);
        this.pathNormal = tag.getBooleanOr("PathNormal", false);
    }

    public static StructurePiece findValidPlacement(
            StructurePiecesBuilder accessor,
            RandomSource random,
            int x,
            int y,
            int z,
            Direction direction) {
        BoundingBox box = anchorBoundingBox(x, y, z, direction, -6, -1, 0, WIDTH, HEIGHT, DEPTH);
        if (box.minY() <= 10 || accessor.findCollisionPiece(box) != null) return null;
        return new LaboratoryPiece(box, direction);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("PathWest", pathWest);
        tag.putBoolean("PathNormal", pathNormal);
    }

    @Override
    public void buildComponent(
            NtmBranchingStart start, StructurePiecesBuilder accessor, RandomSource random) {
        this.pathWest = this.generateChildWest(start, accessor, random, 3, 1) != null;
        this.pathNormal = this.generateChildNormal(start, accessor, random, 6, 1) != null;
    }

    public static final Identifier TEMPLATE_ID = RegistryUtil.id("component/laboratory");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        if (this.pathWest) this.generateAirBox(level, chunkBB, 0, 1, 2, 0, 2, 3);
        if (this.pathNormal) this.generateAirBox(level, chunkBB, 5, 1, 12, 6, 2, 12);
    }
}
