// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.injected.IBulkLightEngine;
import com.hbm.util.ChunkUtil;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ThreadedLevelLightEngine.class)
public abstract class MixinThreadedLevelLightEngine extends LevelLightEngine
        implements IBulkLightEngine {
    protected MixinThreadedLevelLightEngine(
            LightChunkGetter chunks, boolean blockLight, boolean skyLight) {
        super(chunks, blockLight, skyLight);
    }

    @Override
    public CompletableFuture<?> hbm$updateLight(
            LevelChunk chunk, LongList positions, long sectionMask) {
        ThreadedLevelLightEngine engine = (ThreadedLevelLightEngine) (Object) this;
        ServerLevel level = (ServerLevel) chunk.getLevel();
        assert level.getServer().isSameThread();

        if (engine.getClass() != ThreadedLevelLightEngine.class) {
            return ChunkUtil.updateLightIndividually(level, chunk, positions, sectionMask);
        }
        int cx = chunk.getPos().x();
        int cz = chunk.getPos().z();
        if (sectionMask == 0 && positions.isEmpty()) return engine.waitForPendingTasks(cx, cz);
        long emptySections = 0;
        for (long mask = sectionMask; mask != 0; mask &= mask - 1) {
            int index = Long.numberOfTrailingZeros(mask);
            if (chunk.getSection(index).hasOnlyAir()) emptySections |= 1L << index;
        }
        long[] packed = positions.toLongArray();
        var sources = chunk.getSkyLightSources();
        for (long value : packed) {
            sources.update(
                    chunk,
                    BlockPos.getX(value) & 15,
                    BlockPos.getY(value),
                    BlockPos.getZ(value) & 15);
        }
        long emptyMask = emptySections;
        int minSectionY = chunk.getMinSectionY();

        Runnable checks =
                () -> {
                    for (long mask = sectionMask; mask != 0; mask &= mask - 1) {
                        int index = Long.numberOfTrailingZeros(mask);
                        super.updateSectionStatus(
                                SectionPos.of(cx, minSectionY + index, cz),
                                (emptyMask & (1L << index)) != 0);
                    }
                    for (long value : packed) super.checkBlock(BlockPos.of(value));
                };
        if (sectionMask != 0)
            engine.addTask(cx, cz, () -> 0, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, checks);
        else engine.addTask(cx, cz, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, checks);
        return engine.waitForPendingTasks(cx, cz);
    }
}
