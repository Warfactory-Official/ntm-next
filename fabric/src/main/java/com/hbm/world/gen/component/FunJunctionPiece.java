// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.lib.Library;
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

public class FunJunctionPiece extends NtmBranchingPiece {

    private static final int WIDTH = 8, HEIGHT = 6, DEPTH = 12;

    private boolean pathEast;
    private boolean pathNormal;

    private FunJunctionPiece(BoundingBox box, Direction direction) {
        super(HbmStructureTypes.NTM_BUNKER_FUN_JUNCTION.get(), 0, box);
        this.setOrientation(direction);
    }

    public FunJunctionPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_BUNKER_FUN_JUNCTION.get(), context, tag);
        this.pathEast = tag.getBooleanOr("PathEast", false);
        this.pathNormal = tag.getBooleanOr("PathNormal", false);
    }

    public static StructurePiece findValidPlacement(
            StructurePieceAccessor accessor,
            RandomSource random,
            int x,
            int y,
            int z,
            Direction direction) {
        BoundingBox box = anchorBoundingBox(x, y, z, direction, -5, -1, 0, WIDTH, HEIGHT, DEPTH);
        if (box.minY() <= 10 || accessor.findCollisionPiece(box) != null) return null;
        return new FunJunctionPiece(box, direction);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("PathEast", pathEast);
        tag.putBoolean("PathNormal", pathNormal);
    }

    @Override
    public void buildComponent(
            NtmBranchingStart start, StructurePieceAccessor accessor, RandomSource random) {
        this.pathEast = this.generateChildEast(start, accessor, random, 6, 1) != null;
        this.pathNormal = this.generateChildNormal(start, accessor, random, 5, 1) != null;
    }

    public static final Identifier TEMPLATE_ID = Library.id("component/fun_junction");

    @Override
    protected Identifier templateId() {
        return TEMPLATE_ID;
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        if (this.pathEast) this.generateAirBox(level, chunkBB, 7, 1, 5, 7, 2, 6);
        if (this.pathNormal) this.generateAirBox(level, chunkBB, 4, 1, 11, 5, 2, 11);
    }
}
