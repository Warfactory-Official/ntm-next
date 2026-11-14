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

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
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
        var across = direction.getCounterClockWise();
        int nearX = x + across.getStepX() * offsetX + direction.getStepX() * offsetZ;
        int nearZ = z + across.getStepZ() * offsetX + direction.getStepZ() * offsetZ;
        int farX = nearX + across.getStepX() * (sizeX - 1) + direction.getStepX() * (sizeZ - 1);
        int farZ = nearZ + across.getStepZ() * (sizeX - 1) + direction.getStepZ() * (sizeZ - 1);
        return new BoundingBox(
                Math.min(nearX, farX),
                y + offsetY,
                Math.min(nearZ, farZ),
                Math.max(nearX, farX),
                y + offsetY + sizeY - 1,
                Math.max(nearZ, farZ));
    }

    public void buildComponent(
            NtmBranchingStart start, StructurePieceAccessor accessor, RandomSource random) {}

    protected final @Nullable StructurePiece generateChildNormal(
            NtmBranchingStart start,
            StructurePieceAccessor accessor,
            RandomSource random,
            int offset,
            int offsetY) {
        return branch(start, accessor, random, Rotation.NONE, offset, offsetY);
    }

    protected final @Nullable StructurePiece generateChildAntiNormal(
            NtmBranchingStart start,
            StructurePieceAccessor accessor,
            RandomSource random,
            int offset,
            int offsetY) {
        return branch(start, accessor, random, Rotation.CLOCKWISE_180, offset, offsetY);
    }

    protected final @Nullable StructurePiece generateChildWest(
            NtmBranchingStart start,
            StructurePieceAccessor accessor,
            RandomSource random,
            int offset,
            int offsetY) {
        return branch(start, accessor, random, Rotation.CLOCKWISE_90, offset, offsetY);
    }

    protected final @Nullable StructurePiece generateChildEast(
            NtmBranchingStart start,
            StructurePieceAccessor accessor,
            RandomSource random,
            int offset,
            int offsetY) {
        return branch(start, accessor, random, Rotation.COUNTERCLOCKWISE_90, offset, offsetY);
    }

    private @Nullable StructurePiece branch(
            NtmBranchingStart start,
            StructurePieceAccessor accessor,
            RandomSource random,
            Rotation turn,
            int offset,
            int offsetY) {
        var heading = turn.rotate(this.getOrientation());
        var across = heading.getCounterClockWise();
        BoundingBox box = this.getBoundingBox();
        int x = exit(box.minX(), box.maxX(), heading.getStepX(), across.getStepX(), offset);
        int z = exit(box.minZ(), box.maxZ(), heading.getStepZ(), across.getStepZ(), offset);
        return start.grow(accessor, random, x, box.minY() + offsetY, z, heading);
    }

    private static int exit(int min, int max, int forward, int sideways, int offset) {
        if (forward != 0) return forward > 0 ? max + 1 : min - 1;
        return sideways > 0 ? min + offset : max - offset;
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

        boolean open() {
            return maxPlaceCount < 0 || placeCount < maxPlaceCount;
        }
    }

    public static final class NtmBranchingStart {
        private static final int ATTEMPTS = 5;

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

        @Nullable StructurePiece grow(
                StructurePieceAccessor accessor,
                RandomSource random,
                int x,
                int y,
                int z,
                Direction heading) {
            boolean crowded = componentCount >= sizeLimit;
            if (crowded || Math.max(Math.abs(x - originX), Math.abs(z - originZ)) > distanceLimit)
                return null;
            var piece = pick(accessor, random, x, y, z, heading);
            if (piece == null) return null;
            accessor.addPiece(piece);
            pendingChildren.add(piece);
            componentCount++;
            return piece;
        }

        private @Nullable StructurePiece pick(
                StructurePieceAccessor accessor,
                RandomSource random,
                int x,
                int y,
                int z,
                Direction heading) {
            for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                var open = availablePieces.stream().filter(PieceWeight::open).toList();
                int total = open.stream().mapToInt(w -> w.weight).sum();
                if (total <= 0) return null;
                int remaining = random.nextInt(total);
                int first = 0;
                while ((remaining -= open.get(first).weight) >= 0) first++;
                for (int i = first; i < open.size(); i++) {
                    var weight = open.get(i);
                    var piece = weight.factory.create(accessor, random, x, y, z, heading);
                    if (piece == null) continue;
                    weight.placeCount++;
                    return piece;
                }
            }
            return null;
        }
    }
}
