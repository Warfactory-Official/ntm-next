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

package com.hbm.world.gen.dungeon;

import com.hbm.blocks.generic.TrappedBrickBlock;
import com.hbm.util.RegistryUtil;
import com.hbm.world.gen.dungeon.JungleDungeonLayout;
import com.hbm.world.structure.HbmStructureTypes;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class JungleDungeonPiece extends StructurePiece {
    private static final BlockState[] PALETTE = palette();
    private final long seed;
    private final JungleDungeonLayout layout;

    public JungleDungeonPiece(BlockPos anchor, long seed) {
        this(anchor, seed, new JungleDungeonLayout(seed));
    }

    private JungleDungeonPiece(BlockPos anchor, long seed, JungleDungeonLayout layout) {
        super(
                HbmStructureTypes.JUNGLE_DUNGEON_PIECE.get(),
                0,
                new BoundingBox(
                        anchor.getX() - 62 + layout.minX(),
                        anchor.getY(),
                        anchor.getZ() - 62 + layout.minZ(),
                        anchor.getX() - 62 + layout.maxX(),
                        anchor.getY() + 12,
                        anchor.getZ() - 62 + layout.maxZ()));
        this.seed = seed;
        this.layout = layout;
    }

    public JungleDungeonPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.JUNGLE_DUNGEON_PIECE.get(), tag);
        seed = tag.getLong("Seed").orElseThrow();
        layout = new JungleDungeonLayout(seed);
    }

    private static BlockState state(String id) {
        return BuiltInRegistries.BLOCK
                .getOptional(RegistryUtil.id(id))
                .map(Block::defaultBlockState)
                .orElseThrow();
    }

    private static BlockState[] palette() {
        BlockState[] states =
                new BlockState[JungleDungeonLayout.TRAP + TrappedBrickBlock.Trap.values().length];
        states[JungleDungeonLayout.AIR] = Blocks.AIR.defaultBlockState();
        String[] names = {
            "brick_jungle",
            "brick_jungle_cracked",
            "brick_jungle_lava",
            "brick_jungle_fragile",
            "crate_jungle",
            "brick_jungle_circle"
        };
        for (int i = 0; i < names.length; i++)
            states[JungleDungeonLayout.BRICK + i] = state(names[i]);
        for (int i = 0; i < 16; i++)
            states[JungleDungeonLayout.GLYPH + i] = state("brick_jungle_glyph_" + i);
        for (TrappedBrickBlock.Trap trap : TrappedBrickBlock.Trap.values())
            states[JungleDungeonLayout.TRAP + trap.ordinal()] =
                    state("brick_jungle_trap_" + trap.name().toLowerCase(Locale.ROOT));
        return states;
    }

    public JungleDungeonLayout layout() {
        return layout;
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structures,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos) {
        int originX = boundingBox.minX() - layout.minX(),
                originZ = boundingBox.minZ() - layout.minZ();
        int loX = Math.max(boundingBox.minX(), chunkBB.minX()),
                hiX = Math.min(boundingBox.maxX(), chunkBB.maxX());
        int loZ = Math.max(boundingBox.minZ(), chunkBB.minZ()),
                hiZ = Math.min(boundingBox.maxZ(), chunkBB.maxZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = loX; x <= hiX; x++) {
            for (int y = Math.max(boundingBox.minY(), chunkBB.minY());
                    y <= Math.min(boundingBox.maxY(), chunkBB.maxY());
                    y++) {
                for (int z = loZ; z <= hiZ; z++) {
                    int value = layout.get(x - originX, y - boundingBox.minY(), z - originZ);
                    if (value != 0) level.setBlock(pos.set(x, y, z), PALETTE[value], 2);
                }
            }
        }
    }
}
