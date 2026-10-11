// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.blocks.network.GeneratedConductors;
import com.hbm.lib.Library;
import com.hbm.world.gen.nbt.PieceGeometry;
import com.hbm.world.structure.HbmStructureTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class PowerRoomPiece extends NtmBranchingPiece {

    private static final int WIDTH = 12, HEIGHT = 6, DEPTH = 12;
    static final BlockPos SWITCH_POSITION = new BlockPos(1, 2, 5);
    static final BlockPos DETECTOR_POSITION = new BlockPos(6, 2, 1);
    private static final long[] SWITCH = {SWITCH_POSITION.asLong()};
    private static final long[] SWITCH_AND_DETECTOR = {
        SWITCH_POSITION.asLong(), DETECTOR_POSITION.asLong()
    };
    private final int powerType;
    private boolean path;

    private PowerRoomPiece(BoundingBox box, Direction direction, int powerType) {
        super(HbmStructureTypes.NTM_BUNKER_POWER_ROOM.get(), 0, box);
        this.setOrientation(direction);
        this.powerType = powerType;
    }

    public PowerRoomPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_BUNKER_POWER_ROOM.get(), context, tag);
        this.path = tag.getBooleanOr("Path", false);
        this.powerType = tag.getIntOr("PowerType", 0);
    }

    public static StructurePiece findValidPlacement(
            StructurePieceAccessor accessor,
            RandomSource random,
            int x,
            int y,
            int z,
            Direction direction) {
        BoundingBox box = anchorBoundingBox(x, y, z, direction, -4, -1, 0, WIDTH, HEIGHT, DEPTH);
        if (box.minY() <= 10 || accessor.findCollisionPiece(box) != null) return null;
        float chance = random.nextFloat();
        int powerType = chance < 0.2F ? 2 : chance < 0.6F ? 1 : 0;
        return new PowerRoomPiece(box, direction, powerType);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("Path", path);
        tag.putInt("PowerType", powerType);
    }

    @Override
    public void buildComponent(
            NtmBranchingStart start, StructurePieceAccessor accessor, RandomSource random) {
        this.path = this.generateChildEast(start, accessor, random, 4, 1) != null;
    }

    public static final Identifier TEMPLATE_ID = Library.id("component/power_room");

    public static final List<PieceGeometry.Variant> VARIANTS = buildVariants();

    private static List<PieceGeometry.Variant> buildVariants() {
        List<PieceGeometry.Variant> out = new ArrayList<>(3);
        for (int power = 0; power < 3; power++) {
            out.add(new PieceGeometry.Variant("_" + power, Map.of("power", power)));
        }
        return List.copyOf(out);
    }

    @Override
    protected Identifier templateId() {
        return Library.id(TEMPLATE_ID.getPath() + "_" + this.powerType);
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        if (this.path) this.generateAirBox(level, chunkBB, 11, 1, 7, 11, 2, 8);
        GeneratedConductors.record(
                level,
                getWorldPos(0, 0, 0),
                legacyRotation(getOrientation()),
                chunkBB,
                powerType == 1 ? SWITCH_AND_DETECTOR : SWITCH);
    }
}
