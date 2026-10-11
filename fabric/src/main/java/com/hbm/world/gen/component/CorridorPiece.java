// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.lib.Library;
import com.hbm.world.gen.nbt.PieceGeometry;
import com.hbm.world.structure.HbmStructureTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class CorridorPiece extends NtmBranchingPiece {

    private static final int WIDTH = 6, HEIGHT = 6, DEPTH = 7;
    private final int[] decorations = new int[2];
    private boolean path;

    private CorridorPiece(BoundingBox box, Direction direction, RandomSource random) {
        super(HbmStructureTypes.NTM_BUNKER_CORRIDOR.get(), 0, box);
        this.setOrientation(direction);
        this.decorations[0] = random.nextInt(6);
        this.decorations[1] = random.nextInt(6);
    }

    public CorridorPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.NTM_BUNKER_CORRIDOR.get(), context, tag);
        this.path = tag.getBooleanOr("Path", false);
        int[] d = tag.getIntArray("Decorations").orElse(new int[] {0, 0});
        this.decorations[0] = d.length > 0 ? d[0] : 0;
        this.decorations[1] = d.length > 1 ? d[1] : 0;
    }

    public static StructurePiece findValidPlacement(
            StructurePieceAccessor accessor,
            RandomSource random,
            int x,
            int y,
            int z,
            Direction direction) {
        BoundingBox box = anchorBoundingBox(x, y, z, direction, -3, -1, 0, WIDTH, HEIGHT, DEPTH);
        if (box.minY() <= 10 || accessor.findCollisionPiece(box) != null) return null;
        return new CorridorPiece(box, direction, random);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("Path", path);
        tag.putIntArray("Decorations", decorations);
    }

    @Override
    public void buildComponent(
            NtmBranchingStart start, StructurePieceAccessor accessor, RandomSource random) {
        this.path = this.generateChildNormal(start, accessor, random, 3, 1) != null;
    }

    public static final Identifier TEMPLATE_ID = Library.id("component/bunker_corridor");

    public static final List<PieceGeometry.Variant> VARIANTS = buildVariants();

    private static List<PieceGeometry.Variant> buildVariants() {
        List<PieceGeometry.Variant> out = new ArrayList<>(36);
        for (int d0 = 0; d0 < 6; d0++) {
            for (int d1 = 0; d1 < 6; d1++) {
                out.add(
                        new PieceGeometry.Variant(
                                suffix(d0, d1), Map.of("deco0", d0, "deco1", d1)));
            }
        }
        return List.copyOf(out);
    }

    private static String suffix(int deco0, int deco1) {
        return "_" + deco0 + "_" + deco1;
    }

    @Override
    protected Identifier templateId() {
        return Library.id(TEMPLATE_ID.getPath() + suffix(decorations[0], decorations[1]));
    }

    @Override
    protected void buildAfterTemplate(
            WorldGenLevel level, BoundingBox chunkBB, RandomSource random) {
        if (this.path) this.generateAirBox(level, chunkBB, 2, 1, 6, 3, 2, 6);
    }
}
