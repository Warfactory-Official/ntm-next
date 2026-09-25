// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks;

import com.hbm.interfaces.injected.IClimbBoxSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public final class ClimbBoxes {

    private ClimbBoxes() {}

    public static boolean counts(BlockState state) {
        return state.getBlock() instanceof ClimbBox block && block.climbBox(state) != null;
    }

    public static @Nullable BlockPos find(Level level, AABB bb) {
        int x0 = Mth.floor(bb.minX - ClimbBox.REACH), x1 = Mth.floor(bb.maxX + ClimbBox.REACH);
        int y0 = Mth.floor(bb.minY), y1 = Mth.floor(bb.maxY);
        int z0 = Mth.floor(bb.minZ - ClimbBox.REACH), z1 = Mth.floor(bb.maxZ + ClimbBox.REACH);
        for (int sx = SectionPos.blockToSectionCoord(x0);
                sx <= SectionPos.blockToSectionCoord(x1);
                sx++) {
            for (int sz = SectionPos.blockToSectionCoord(z0);
                    sz <= SectionPos.blockToSectionCoord(z1);
                    sz++) {
                ChunkAccess chunk = level.getChunk(sx, sz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                LevelChunkSection[] sections = chunk.getSections();
                for (int sy = SectionPos.blockToSectionCoord(y0);
                        sy <= SectionPos.blockToSectionCoord(y1);
                        sy++) {
                    int index = chunk.getSectionIndexFromSectionY(sy);
                    if (index < 0 || index >= sections.length) continue;
                    LevelChunkSection section = sections[index];
                    if (((IClimbBoxSection) section).hbm$climbBoxes() == 0) continue;
                    int bx = SectionPos.sectionToBlockCoord(sx),
                            by = SectionPos.sectionToBlockCoord(sy);
                    int bz = SectionPos.sectionToBlockCoord(sz);
                    BlockPos hit =
                            scan(
                                    section,
                                    bb,
                                    Math.max(x0, bx),
                                    Math.max(y0, by),
                                    Math.max(z0, bz),
                                    Math.min(x1, bx + 15),
                                    Math.min(y1, by + 15),
                                    Math.min(z1, bz + 15));
                    if (hit != null) return hit;
                }
            }
        }
        return null;
    }

    private static @Nullable BlockPos scan(
            LevelChunkSection section, AABB bb, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                    if (!(state.getBlock() instanceof ClimbBox block)) continue;
                    AABB box = block.climbBox(state);
                    if (box != null
                            && bb.intersects(
                                    x + box.minX,
                                    y + box.minY,
                                    z + box.minZ,
                                    x + box.maxX,
                                    y + box.maxY,
                                    z + box.maxZ)) {
                        return new BlockPos(x, y, z);
                    }
                }
            }
        }
        return null;
    }
}
