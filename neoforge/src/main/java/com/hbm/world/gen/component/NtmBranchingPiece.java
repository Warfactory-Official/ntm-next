// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.jspecify.annotations.Nullable;

public abstract class NtmBranchingPiece extends NtmComponentPiece {

    protected NtmBranchingPiece(StructurePieceType type, int genDepth, BoundingBox boundingBox) {
        super(type, genDepth, boundingBox);
    }

    protected NtmBranchingPiece(
            StructurePieceType type, StructurePieceSerializationContext context, CompoundTag tag) {
        super(type, context, tag);
    }

    private static int coordModeOf(Direction direction) {
        return direction.get2DDataValue();
    }

    protected static BoundingBox anchorBoundingBox(
            int x,
            int y,
            int z,
            Direction direction,
            int offsetX,
            int offsetY,
            int offsetZ,
            int sizeX,
            int sizeY,
            int sizeZ) {
        int mode = coordModeOf(direction);
        return switch (mode) {
            case 0 ->
                    new BoundingBox(
                            x + offsetX,
                            y + offsetY,
                            z + offsetZ,
                            x + sizeX - 1 + offsetX,
                            y + sizeY - 1 + offsetY,
                            z + sizeZ - 1 + offsetZ);
            case 1 ->
                    new BoundingBox(
                            x - sizeZ + 1 - offsetZ,
                            y + offsetY,
                            z + offsetX,
                            x - offsetZ,
                            y + sizeY - 1 + offsetY,
                            z + sizeX - 1 + offsetX);
            case 2 ->
                    new BoundingBox(
                            x - sizeX + 1 - offsetX,
                            y + offsetY,
                            z - sizeZ + 1 - offsetZ,
                            x - offsetX,
                            y + sizeY - 1 + offsetY,
                            z + offsetZ);
            default ->
                    new BoundingBox(
                            x + offsetZ,
                            y + offsetY,
                            z - sizeX + 1 - offsetX,
                            x + sizeZ - 1 + offsetZ,
                            y + sizeY - 1 + offsetY,
                            z - offsetX);
        };
    }

    private static @Nullable StructurePiece generatePiece(
            NtmBranchingStart start,
            StructurePieceAccessor accessor,
            RandomSource random,
            int x,
            int y,
            int z,
            Direction direction) {
        List<PieceWeight> weights = start.availablePieces;
        for (int attempt = 0; attempt < 5; attempt++) {
            int totalWeight = 0;
            for (PieceWeight w : weights) {
                if (w.maxPlaceCount < 0 || w.placeCount < w.maxPlaceCount) totalWeight += w.weight;
            }
            if (totalWeight <= 0) return null;

            int roll = random.nextInt(totalWeight);
            for (PieceWeight w : weights) {
                if (w.maxPlaceCount >= 0 && w.placeCount >= w.maxPlaceCount) continue;
                roll -= w.weight;
                if (roll < 0) {
                    StructurePiece piece = w.factory.create(accessor, random, x, y, z, direction);
                    if (piece != null) {
                        w.placeCount++;
                        return piece;
                    }
                    break;
                }
            }
        }
        return null;
    }

    public void buildComponent(
            NtmBranchingStart start, StructurePieceAccessor accessor, RandomSource random) {}

    protected final @Nullable StructurePiece generateChildNormal(
            NtmBranchingStart start,
            StructurePieceAccessor accessor,
            RandomSource random,
            int offset,
            int offsetY) {
        BoundingBox box = this.getBoundingBox();
        int mode = coordModeOf(this.getOrientation());
        return switch (mode) {
            case 0 ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.minX() + offset,
                            box.minY() + offsetY,
                            box.maxZ() + 1,
                            Direction.SOUTH);
            case 1 ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.minX() - 1,
                            box.minY() + offsetY,
                            box.minZ() + offset,
                            Direction.WEST);
            case 2 ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.maxX() - offset,
                            box.minY() + offsetY,
                            box.minZ() - 1,
                            Direction.NORTH);
            default ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.maxX() + 1,
                            box.minY() + offsetY,
                            box.maxZ() - offset,
                            Direction.EAST);
        };
    }

    protected final @Nullable StructurePiece generateChildAntiNormal(
            NtmBranchingStart start,
            StructurePieceAccessor accessor,
            RandomSource random,
            int offset,
            int offsetY) {
        BoundingBox box = this.getBoundingBox();
        int mode = coordModeOf(this.getOrientation());
        return switch (mode) {
            case 0 ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.maxX() - offset,
                            box.minY() + offsetY,
                            box.minZ() - 1,
                            Direction.NORTH);
            case 1 ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.maxX() + 1,
                            box.minY() + offsetY,
                            box.maxZ() - offset,
                            Direction.EAST);
            case 2 ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.minX() + offset,
                            box.minY() + offsetY,
                            box.maxZ() + 1,
                            Direction.SOUTH);
            default ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.minX() - 1,
                            box.minY() + offsetY,
                            box.minZ() + offset,
                            Direction.WEST);
        };
    }

    protected final @Nullable StructurePiece generateChildWest(
            NtmBranchingStart start,
            StructurePieceAccessor accessor,
            RandomSource random,
            int offset,
            int offsetY) {
        BoundingBox box = this.getBoundingBox();
        int mode = coordModeOf(this.getOrientation());
        return switch (mode) {
            case 0 ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.minX() - 1,
                            box.minY() + offsetY,
                            box.minZ() + offset,
                            Direction.WEST);
            case 1 ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.maxX() - offset,
                            box.minY() + offsetY,
                            box.minZ() - 1,
                            Direction.NORTH);
            case 2 ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.maxX() + 1,
                            box.minY() + offsetY,
                            box.maxZ() - offset,
                            Direction.EAST);
            default ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.minX() + offset,
                            box.minY() + offsetY,
                            box.maxZ() + 1,
                            Direction.SOUTH);
        };
    }

    protected final @Nullable StructurePiece generateChildEast(
            NtmBranchingStart start,
            StructurePieceAccessor accessor,
            RandomSource random,
            int offset,
            int offsetY) {
        BoundingBox box = this.getBoundingBox();
        int mode = coordModeOf(this.getOrientation());
        return switch (mode) {
            case 0 ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.maxX() + 1,
                            box.minY() + offsetY,
                            box.maxZ() - offset,
                            Direction.EAST);
            case 1 ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.minX() + offset,
                            box.minY() + offsetY,
                            box.maxZ() + 1,
                            Direction.SOUTH);
            case 2 ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.minX() - 1,
                            box.minY() + offsetY,
                            box.minZ() + offset,
                            Direction.WEST);
            default ->
                    generateAndAddPiece(
                            start,
                            accessor,
                            random,
                            box.maxX() - offset,
                            box.minY() + offsetY,
                            box.minZ() - 1,
                            Direction.NORTH);
        };
    }

    private @Nullable StructurePiece generateAndAddPiece(
            NtmBranchingStart start,
            StructurePieceAccessor accessor,
            RandomSource random,
            int x,
            int y,
            int z,
            Direction direction) {
        if (start.componentCount >= start.sizeLimit) return null;
        if (Math.abs(x - start.originX) > start.distanceLimit
                || Math.abs(z - start.originZ) > start.distanceLimit) return null;

        StructurePiece piece = generatePiece(start, accessor, random, x, y, z, direction);
        if (piece != null) {
            accessor.addPiece(piece);
            start.pendingChildren.add(piece);
            start.componentCount++;
        }
        return piece;
    }

    @FunctionalInterface
    public interface PieceFactory {
        @Nullable StructurePiece create(
                StructurePieceAccessor accessor,
                RandomSource random,
                int x,
                int y,
                int z,
                Direction direction);
    }

    public static final class PieceWeight {
        public final int weight;
        public final int maxPlaceCount;
        public final PieceFactory factory;
        public int placeCount;

        public PieceWeight(int weight, int maxPlaceCount, PieceFactory factory) {
            this.weight = weight;
            this.maxPlaceCount = maxPlaceCount;
            this.factory = factory;
        }
    }

    public static final class NtmBranchingStart {
        public final List<StructurePiece> pendingChildren = new ArrayList<>();
        public final List<PieceWeight> availablePieces;
        public final int sizeLimit;
        public final int distanceLimit;
        public final int originX;
        public final int originZ;
        public int componentCount;

        public NtmBranchingStart(
                List<PieceWeight> availablePieces,
                int sizeLimit,
                int distanceLimit,
                int originX,
                int originZ) {
            this.availablePieces = availablePieces;
            this.sizeLimit = sizeLimit;
            this.distanceLimit = distanceLimit;
            this.originX = originX;
            this.originZ = originZ;
        }
    }
}
