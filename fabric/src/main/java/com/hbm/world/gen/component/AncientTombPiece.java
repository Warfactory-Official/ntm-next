// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen.component;

import com.hbm.lib.Library;
import com.hbm.tags.HbmBlockTags;
import com.hbm.world.NtmWorldgenFields;
import com.hbm.world.WorldgenHash;
import com.hbm.world.gen.nbt.WeightedOption;
import com.hbm.world.gen.nbt.WeightedSelector;
import com.hbm.world.structure.HbmStructureTypes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class AncientTombPiece extends NtmComponentPiece {

    private static final int PYRAMID_SIZE = 15;

    private static final double TUNNEL_ANGLE_STEP = 2.0 * Math.PI / 32.0;
    private static final long CONCRETE_DOMAIN =
            WorldgenHash.identifier(Library.id("structure/ancient_tomb_concrete"));

    private final int originX, originZ, yOff;
    private final int[] spikeX, spikeZ, spikeY;

    public AncientTombPiece(int x, int z, int yOff, int[] spikeX, int[] spikeZ, int[] spikeY) {
        super(HbmStructureTypes.ANCIENT_TOMB_PIECE.get(), 0, boundingBoxFor(x, z, yOff, spikeY));

        this.setOrientation(null);
        this.originX = x;
        this.originZ = z;
        this.yOff = yOff;
        this.spikeX = spikeX;
        this.spikeZ = spikeZ;
        this.spikeY = spikeY;
    }

    public AncientTombPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(HbmStructureTypes.ANCIENT_TOMB_PIECE.get(), context, tag);
        this.setOrientation(null);
        this.originX = tag.getIntOr("TX", 0);
        this.originZ = tag.getIntOr("TZ", 0);
        this.yOff = tag.getIntOr("YOff", 30);
        this.spikeX = tag.getIntArray("SpikeX").orElse(new int[0]);
        this.spikeZ = tag.getIntArray("SpikeZ").orElse(new int[0]);
        this.spikeY = tag.getIntArray("SpikeY").orElse(new int[0]);
    }

    public static BoundingBox boundingBoxFor(int x, int z, int yOff, int[] spikeY) {
        int minSpikeY = yOff, maxSpikeY = yOff;
        for (int sy : spikeY) {
            minSpikeY = Math.min(minSpikeY, sy);
            maxSpikeY = Math.max(maxSpikeY, sy);
        }

        int minY = Math.min(14, minSpikeY - 3);
        int maxY = Math.max(yOff + PYRAMID_SIZE + 1, maxSpikeY + 8);
        return new BoundingBox(x - 30, minY, z - 30, x + 30, maxY, z + 30);
    }

    private static boolean isTombMaterial(BlockState state) {
        return state.isAir() || state.is(HbmBlockTags.ANCIENT_TOMB_MATERIAL);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("TX", originX);
        tag.putInt("TZ", originZ);
        tag.putInt("YOff", yOff);
        tag.putIntArray("SpikeX", spikeX);
        tag.putIntArray("SpikeZ", spikeZ);
        tag.putIntArray("SpikeY", spikeY);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos) {
        int x = originX, z = originZ;
        long concreteSeed = NtmWorldgenFields.get(level).domainSeed(CONCRETE_DOMAIN);

        for (int iy = PYRAMID_SIZE; iy > 0; iy--) {
            int range = PYRAMID_SIZE - iy;
            int wy = yOff + iy;
            if (wy < chunkBB.minY() || wy > chunkBB.maxY()) continue;
            int minX = Math.max(-range, chunkBB.minX() - x);
            int maxX = Math.min(range, chunkBB.maxX() - x);
            int minZ = Math.max(-range, chunkBB.minZ() - z);
            int maxZ = Math.min(range, chunkBB.maxZ() - z);
            for (int ix = minX; ix <= maxX; ix++) {
                boolean xEdge = ix <= -range + 1 || ix >= range - 1;
                for (int iz = minZ; iz <= maxZ; iz++) {
                    boolean zEdge = iz <= -range + 1 || iz >= range - 1;
                    if (xEdge && zEdge) {
                        this.placeBlock(
                                level, States.REINFORCED_STONE, x + ix, wy, z + iz, chunkBB);
                    } else if (iy == 1) {
                        this.placeBlock(level, States.CONCRETE_SMOOTH, x + ix, wy, z + iz, chunkBB);
                    } else if (xEdge || zEdge) {
                        this.placeBlock(level, States.CONCRETE_SMOOTH, x + ix, wy, z + iz, chunkBB);
                    } else {
                        this.placeBlock(
                                level, Blocks.AIR.defaultBlockState(), x + ix, wy, z + iz, chunkBB);
                    }
                }
            }
        }

        concreteBox(level, chunkBB, concreteSeed, x - 2, yOff + 2, z - 2, x + 2, yOff + 5, z + 2);
        this.placeBlock(level, States.MARKED, x + 2, yOff + 3, z, chunkBB);
        this.placeBlock(level, States.MARKED, x - 2, yOff + 3, z, chunkBB);
        this.placeBlock(level, States.MARKED, x, yOff + 3, z + 2, chunkBB);
        this.placeBlock(level, States.MARKED, x, yOff + 3, z - 2, chunkBB);

        fixedBox(
                level,
                chunkBB,
                x + 5,
                yOff + 2,
                z + 5,
                x + 5,
                yOff + 8,
                z + 5,
                States.CONCRETE_PILLAR,
                States.CONCRETE_PILLAR);
        fixedBox(
                level,
                chunkBB,
                x + 5,
                yOff + 2,
                z - 5,
                x + 5,
                yOff + 8,
                z - 5,
                States.CONCRETE_PILLAR,
                States.CONCRETE_PILLAR);
        fixedBox(
                level,
                chunkBB,
                x - 5,
                yOff + 2,
                z - 5,
                x - 5,
                yOff + 8,
                z - 5,
                States.CONCRETE_PILLAR,
                States.CONCRETE_PILLAR);
        fixedBox(
                level,
                chunkBB,
                x - 5,
                yOff + 2,
                z + 5,
                x - 5,
                yOff + 8,
                z + 5,
                States.CONCRETE_PILLAR,
                States.CONCRETE_PILLAR);

        for (int i = 0; i < spikeX.length; i++) {
            for (int j = 0; j < 7; j++) {
                if (chunkBB.isInside(spikeX[i], spikeY[i] + j, spikeZ[i])) {
                    this.placeBlock(
                            level, States.DECO_STEEL, spikeX[i], spikeY[i] + j, spikeZ[i], chunkBB);
                }
            }
        }

        int tunnelStart = 19;
        int step = 0;
        for (int i = tunnelStart; i < yOff + 2; i++) {
            double angle = step * TUNNEL_ANGLE_STEP;
            int ix = (int) Math.floor(10.0 * Math.cos(angle));
            int iz = (int) Math.floor(-10.0 * Math.sin(angle));
            int h = i < yOff ? 3 : 2;
            BlockState carve = i > 40 ? Blocks.AIR.defaultBlockState() : States.GAS_RADON_TOMB;
            fixedBox(
                    level,
                    chunkBB,
                    x + ix - 1,
                    i,
                    z + iz - 1,
                    x + ix + 1,
                    i + h - 1,
                    z + iz + 1,
                    carve,
                    carve);

            claddingPass(
                    level,
                    chunkBB,
                    concreteSeed,
                    x + ix - 2,
                    x + ix + 2,
                    i - 1,
                    i + 3,
                    z + iz - 2,
                    z + iz + 2);
            step++;
        }

        claddingPass(
                level,
                chunkBB,
                concreteSeed,
                x + 4,
                x + 7,
                tunnelStart,
                tunnelStart + 4,
                z - 2,
                z + 2);

        int y = 20;
        int size = 5, cladding = 4, core = 3;
        int outerLo = y - size, outerHi = y + size;
        int innerLo = y - cladding, innerHi = y + cladding;
        int coreLo = y - core, coreHi = y + core;

        concreteBox(
                level,
                chunkBB,
                concreteSeed,
                x - size,
                outerLo,
                z - size,
                x - size,
                outerHi,
                z + size);
        concreteBox(
                level,
                chunkBB,
                concreteSeed,
                x - size,
                outerLo,
                z - size,
                x + size,
                outerLo,
                z + size);
        concreteBox(
                level,
                chunkBB,
                concreteSeed,
                x - size,
                outerLo,
                z - size,
                x + size,
                outerHi,
                z - size);
        concreteBox(
                level,
                chunkBB,
                concreteSeed,
                x + size,
                outerLo,
                z - size,
                x + size,
                outerHi,
                z + size);
        concreteBox(
                level,
                chunkBB,
                concreteSeed,
                x - size,
                outerHi,
                z - size,
                x + size,
                outerHi,
                z + size);
        concreteBox(
                level,
                chunkBB,
                concreteSeed,
                x - size,
                outerLo,
                z + size,
                x + size,
                outerHi,
                z + size);

        fixedBox(
                level,
                chunkBB,
                x - cladding,
                innerLo,
                z - cladding,
                x - cladding,
                innerHi,
                z + cladding,
                States.BRICK_OBSIDIAN,
                States.BRICK_OBSIDIAN);
        fixedBox(
                level,
                chunkBB,
                x - cladding,
                innerLo,
                z - cladding,
                x + cladding,
                innerLo,
                z + cladding,
                States.BRICK_OBSIDIAN,
                States.BRICK_OBSIDIAN);
        fixedBox(
                level,
                chunkBB,
                x - cladding,
                innerLo,
                z - cladding,
                x + cladding,
                innerHi,
                z - cladding,
                States.BRICK_OBSIDIAN,
                States.BRICK_OBSIDIAN);
        fixedBox(
                level,
                chunkBB,
                x + cladding,
                innerLo,
                z - cladding,
                x + cladding,
                innerHi,
                z + cladding,
                States.BRICK_OBSIDIAN,
                States.BRICK_OBSIDIAN);
        fixedBox(
                level,
                chunkBB,
                x - cladding,
                innerHi,
                z - cladding,
                x + cladding,
                innerHi,
                z + cladding,
                States.BRICK_OBSIDIAN,
                States.BRICK_OBSIDIAN);
        fixedBox(
                level,
                chunkBB,
                x - cladding,
                innerLo,
                z + cladding,
                x + cladding,
                innerHi,
                z + cladding,
                States.BRICK_OBSIDIAN,
                States.BRICK_OBSIDIAN);

        fixedBox(
                level,
                chunkBB,
                x - core,
                coreLo,
                z - core,
                x + core,
                coreHi,
                z + core,
                States.ANCIENT_SCRAP,
                States.ANCIENT_SCRAP);

        concreteBox(level, chunkBB, concreteSeed, x + 6, y - 2, z - 1, x + 7, y - 2, z + 1);
        fixedBox(
                level,
                chunkBB,
                x + 4,
                y - 1,
                z - 1,
                x + 8,
                y + 1,
                z + 1,
                States.GAS_RADON_TOMB,
                States.GAS_RADON_TOMB);
        concreteBox(level, chunkBB, concreteSeed, x + 6, y + 2, z - 1, x + 7, y + 2, z + 1);
    }

    private void claddingPass(
            WorldGenLevel level,
            BoundingBox chunkBB,
            long concreteSeed,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ) {
        for (int dx = Math.max(minX, chunkBB.minX()); dx <= Math.min(maxX, chunkBB.maxX()); dx++) {
            for (int dy = Math.max(minY, chunkBB.minY());
                    dy <= Math.min(maxY, chunkBB.maxY());
                    dy++) {
                for (int dz = Math.max(minZ, chunkBB.minZ());
                        dz <= Math.min(maxZ, chunkBB.maxZ());
                        dz++) {
                    BlockState b = this.getBlock(level, dx, dy, dz, chunkBB);
                    if (!isTombMaterial(b)) {
                        this.placeBlock(
                                level,
                                TombConcreteSelector.select(concreteSeed, dx, dy, dz),
                                dx,
                                dy,
                                dz,
                                chunkBB);
                    }
                }
            }
        }
    }

    private void concreteBox(
            WorldGenLevel level,
            BoundingBox chunkBB,
            long concreteSeed,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ) {
        for (int y = Math.max(minY, chunkBB.minY()); y <= Math.min(maxY, chunkBB.maxY()); y++) {
            for (int x = Math.max(minX, chunkBB.minX()); x <= Math.min(maxX, chunkBB.maxX()); x++) {
                for (int z = Math.max(minZ, chunkBB.minZ());
                        z <= Math.min(maxZ, chunkBB.maxZ());
                        z++) {
                    this.placeBlock(
                            level,
                            TombConcreteSelector.select(concreteSeed, x, y, z),
                            x,
                            y,
                            z,
                            chunkBB);
                }
            }
        }
    }

    private void fixedBox(
            WorldGenLevel level,
            BoundingBox chunkBB,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            BlockState edge,
            BlockState fill) {
        for (int y = Math.max(minY, chunkBB.minY()); y <= Math.min(maxY, chunkBB.maxY()); y++) {
            for (int x = Math.max(minX, chunkBB.minX()); x <= Math.min(maxX, chunkBB.maxX()); x++) {
                for (int z = Math.max(minZ, chunkBB.minZ());
                        z <= Math.min(maxZ, chunkBB.maxZ());
                        z++) {
                    BlockState state =
                            y == minY || y == maxY || x == minX || x == maxX || z == minZ
                                            || z == maxZ
                                    ? edge
                                    : fill;
                    this.placeBlock(level, state, x, y, z, chunkBB);
                }
            }
        }
    }

    private static final class TombConcreteSelector extends BlockSelector
            implements WeightedSelector {
        private TombConcreteSelector() {}

        @Override
        public List<WeightedOption> options() {
            return Options.VALUE;
        }

        @Override
        public void next(RandomSource random, int worldX, int worldY, int worldZ, boolean isEdge) {
            this.next =
                    switch (random.nextInt(3)) {
                        case 0 -> States.BRICK_CONCRETE;
                        case 1 -> States.BRICK_CONCRETE_BROKEN;
                        default -> States.BRICK_CONCRETE_CRACKED;
                    };
        }

        private static BlockState select(long domainSeed, int worldX, int worldY, int worldZ) {
            return switch (WorldgenHash.bounded(
                    WorldgenHash.position(domainSeed, worldX, worldY, worldZ, 0), 3)) {
                case 0 -> States.BRICK_CONCRETE;
                case 1 -> States.BRICK_CONCRETE_BROKEN;
                default -> States.BRICK_CONCRETE_CRACKED;
            };
        }

        private static final class Options {
            private static final List<WeightedOption> VALUE =
                    List.of(
                            new WeightedOption(States.BRICK_CONCRETE, 1),
                            new WeightedOption(States.BRICK_CONCRETE_BROKEN, 1),
                            new WeightedOption(States.BRICK_CONCRETE_CRACKED, 1));
        }
    }

    private static final class States {
        private static final BlockState REINFORCED_STONE = hbmState("reinforced_stone");
        private static final BlockState CONCRETE_SMOOTH = hbmState("concrete_smooth");
        private static final BlockState CONCRETE_PILLAR = hbmState("concrete_pillar");
        private static final BlockState GAS_RADON_TOMB = hbmState("gas_radon_tomb");
        private static final BlockState BRICK_OBSIDIAN = hbmState("brick_obsidian");
        private static final BlockState MARKED = hbmState("brick_concrete_marked");
        private static final BlockState DECO_STEEL = hbmState("deco_steel");
        private static final BlockState ANCIENT_SCRAP = hbmState("ancient_scrap");
        private static final BlockState BRICK_CONCRETE = hbmState("brick_concrete");
        private static final BlockState BRICK_CONCRETE_BROKEN = hbmState("brick_concrete_broken");
        private static final BlockState BRICK_CONCRETE_CRACKED = hbmState("brick_concrete_cracked");
    }
}
